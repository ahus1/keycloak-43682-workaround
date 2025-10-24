/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.redhat.keycloak.mappers;

import org.jboss.logging.Logger;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.broker.saml.mappers.AbstractAttributeToRoleMapper;
import org.keycloak.broker.saml.mappers.AttributeToRoleMapper;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * Optimized attribute-to-role-mapper that will skip writing to the database layer if the user is already set up with the correct roles.
 * This is a hotfix until <a href="https://github.com/keycloak/keycloak/issues/43682">#43682</a> is available.
 */
public class OptimizedAttributeToRoleMapper extends AttributeToRoleMapper {
    private static final Logger LOG = Logger.getLogger(AbstractAttributeToRoleMapper.class);

    public static final String PROVIDER_ID = AttributeToRoleMapper.PROVIDER_ID + "-optimized";

    @Override
    public String getHelpText() {
        return super.getHelpText() + " (optimized)";
    }

    @Override
    public String getDisplayType() {
        return super.getDisplayType() + " (optimized)";
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        RoleModel role = this.getRole(realm, mapperModel);
        if (role == null) {
            return;
        }

        String roleName = mapperModel.getConfig().get(ConfigConstants.ROLE);
        // KEYCLOAK-8730 if a previous mapper has already granted the same role, skip the checks so we don't accidentally remove a valid role.
        if (!context.hasMapperGrantedRole(roleName)) {
            if (this.applies(mapperModel, context)) {
                context.addMapperGrantedRole(roleName);
                if ((!role.isClientRole() && user.getRealmRoleMappingsStream().noneMatch(r -> r.equals(role)))
                        || (role.isClientRole() && user.getClientRoleMappingsStream(session.clients().getClientById(realm, role.getContainerId())).noneMatch(r -> r.equals(role)))) {
                    user.grantRole(role);
                }
            } else {
                if ((!role.isClientRole() && user.getRealmRoleMappingsStream().anyMatch(r -> r.equals(role)))
                        || (role.isClientRole() && user.getClientRoleMappingsStream(session.clients().getClientById(realm, role.getContainerId())).anyMatch(r -> r.equals(role)))) {
                    user.deleteRoleMapping(role);
                }
            }
        }
    }

    /**
     * Obtains the {@link RoleModel} corresponding the role configured in the specified
     * {@link IdentityProviderMapperModel}.
     * If the role doesn't correspond to one of the realm's client roles or to one of the realm's roles, this method
     * returns {@code null}.
     *
     * @param realm       a reference to the realm.
     * @param mapperModel a reference to the {@link IdentityProviderMapperModel} containing the configured role.
     * @return the {@link RoleModel} that corresponds to the mapper model role or {@code null}, if the role could not be
     * found
     */
    private RoleModel getRole(final RealmModel realm, final IdentityProviderMapperModel mapperModel) {
        String roleName = mapperModel.getConfig().get(ConfigConstants.ROLE);
        RoleModel role = KeycloakModelUtils.getRoleFromString(realm, roleName);
        if (role == null) {
            LOG.warnf("Unable to find role '%s' for mapper '%s' on realm '%s'.", roleName, mapperModel.getName(),
                    realm.getName());
        }
        return role;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
