![Create Release](https://github.com/chrisime/CRooD/workflows/Create%20Release/badge.svg) ![Publish to Bintray](https://github.com/chrisime/CRooD/workflows/Publish%20to%20Bintray/badge.svg)

# CRooD

An easy-to-use CRUD service/repository written in Kotlin and based on [jOOQ](https://jooq.org/) that in conjunction 
with the included `DomainGenerator` helps you to avoid writing boiler-plate code.

## Setup

### Gradle based project

If you already use the [jOOQ gradle plugin](https://github.com/etiennestuder/gradle-jooq-plugin/) it's easy to use
_CRooD_. Use `xyz.chrisime.crood.codegen.DomainGenerator` for the generator and 
`xyz.chrisime.crood.codegen.DomainGeneratorStrategy` for the generator strategy. It's recommended to use
`isNullableAnnotation` and `isNonnullAnnotation` since it helps you to avoid `NullPointerException`s.

```kotlin
jooq {
    version.set("3.14.4")
    edition.set(nu.studer.gradle.jooq.JooqEdition.OSS)

    configurations {
        create("main") {
            jooqConfiguration.apply {
                generateSchemaSourceOnCompilation.set(false)
                logging = org.jooq.meta.jaxb.Logging.INFO
                jdbc.apply {
                    driver = "org.postgresql.Driver"
                    url = "jdbc:postgresql://localhost/shopping_list"
                    user = "user"
                    password = "password"
                }
                generator.apply {
                    name = "xyz.chrisime.crood.codegen.DomainGenerator"
                    strategy.name = "xyz.chrisime.crood.codegen.DomainGeneratorStrategy"
                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = "public"
                    }
                    generate.apply {
                        isNullableAnnotation = true
                        isNonnullAnnotation = true
                        isValidationAnnotations = true
                        isDeprecated = false
                        isRecords = true
                        isImmutablePojos = true
                        isFluentSetters = false
                    }
                    target.apply {
                        packageName = "com.github.chrisime"
                        directory = "${project.buildDir}/generated/jooq"
                    }
                }
            }
        }
    }
}
```

### Maven based project

**TODO**

## How to use CRooD

### Java
```java
public class MyTestRepo extends CRUDService<TestRecord, Long, TestDomain> {
    
    private DSLContext dslContext;
    
    public MyTestRepo(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    // create, update, delete, exists and other methods are now available in a typesafe way
    
}

```

### Kotlin
```kotlin
class MyTestRepo(dslContext: DSLContext): CRUDService<TestRecord, Long, TestDomain>(dslContext) {
    // create, update, delete, exists and other methods are now available in a typesafe way
}
```
