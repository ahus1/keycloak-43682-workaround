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

2. Place the file in the `providers` folder of your Red Hat Build of Keycloak installation on each server.

3. Restart each server.
4. Update the existing mappers via the database by executing:
   ```sql
   update identity_provider_mapper set idp_mapper_name = 'saml-role-idp-mapper-optimized' where idp_mapper_name = 'saml-role-idp-mapper'
   ```
   Verify that the mappers for the identity provider now show "SAML Attribute to Role (optimized)" for all role mappers that previously showed "SAML Attribute to Role"

## Analyzing if the workaround is effective

To analyze if the change is effective, start the Keycloak server with SQL debug logging enabled as follows: 

```
bin/kc.sh start --log-level="INFO,org.hibernate.SQL:debug"
```

or an equivalent change for the key `log-level` to the `keycloak.conf` file or the environment variable `KC_LOG_LEVEL`. 

Once the change is implemented, there should be fewer statements of the following when a user logs in via a SAML IDP: 

``select urme1_0.ROLE_ID,urme1_0.USER_ID from USER_ROLE_MAPPING urme1_0 with (updlock,holdlock,rowlock) where urme1_0.USER_ID=? and urme1_0.ROLE_ID=?``

* If the user that logs in already has the correct roles, there should be none of these select statements
* If the user has a role in Keycloak, and it is not present as an attribute in SAML, there should be one statement for each role that is about to be removed.  

## Uninstalling the workaround

1. Update the existing mappers via the database by executing:
   ```sql
   update identity_provider_mapper set idp_mapper_name = 'saml-role-idp-mapper' where idp_mapper_name = 'saml-role-idp-mapper-optimized'
   ```
   Verify that the mappers for the identity provider now show "SAML Attribute to Role" for all role mappers that previously showed "SAML Attribute to Role (optimized)"
2. Remove the file `optimized-attribute-to-role-mapper.jar` from the providers folder on each server
3. Restart each server. 

## Creating a source archive

```
git archive --format zip --output target/optimized-attribute-to-role-mapper.zip HEAD
```

