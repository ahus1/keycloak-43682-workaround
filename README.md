# Optimized Attribute Role Mapper

## What is it?

This optimized mapper will lock roles in the database if no changes are needed for the existing user's roles.

This can be used to reduce deadlocks in the database.

It is a workaround for the upstream issue https://github.com/keycloak/keycloak/issues/43682

## Installing the workaround

1. Package the JAR file: 
   ```
   mvn package
   ```
   This will create a file `target/optimized-attribute-to-role-mapper.jar`

2. Place the file in the `providers` folder of your Red Hat Build of Keycloak installation.

3. Restart the server.
4. Update the existing mappers via the database by executing:
   ```sql
   update identity_provider_mapper set idp_mapper_name = 'saml-role-idp-mapper-optimized' where idp_mapper_name = 'saml-role-idp-mapper'
   ```
   Verify that the mappers for the identity provider now show "SAML Attribute to Role (optimized)" for all role mappers that previously showed "SAML Attribute to Role"

## Uninstalling the workaround

1. Update the existing mappers via the database by executing:
   ```sql
   update identity_provider_mapper set idp_mapper_name = 'saml-role-idp-mapper' where idp_mapper_name = 'saml-role-idp-mapper-optimized'
   ```
   Verify that the mappers for the identity provider now show "SAML Attribute to Role" for all role mappers that previously showed "SAML Attribute to Role (optimized)"
2. Remove the file `optimized-attribute-to-role-mapper.jar` from the providers folder
3. Restart the server. 

## Creating a source archive

```
git archive --format zip --output target/optimized-attribute-to-role-mapper.zip HEAD
```

