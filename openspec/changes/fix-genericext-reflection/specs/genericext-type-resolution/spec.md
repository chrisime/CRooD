# genericext-type-resolution

Type-resolution helpers in `GenericExt` that extract reified generic superclass type arguments and instantiate classes from them. `CRooDService` depends on them to derive its record and domain types.

## ADDED Requirements

### Requirement: getClassAtIndex resolves the reified generic type argument from the receiver's generic superclass
The extension function `getClassAtIndex<T>(index)` SHALL resolve the `index`-th type argument from the receiver's (not `java.lang.Object`'s) generic superclass chain and return it as the requested class type.

Feature: genericext-type-resolution
Rule: Type arguments are read from the actual receiver subclass, not from java.lang.Object

#### Scenario: Resolve a type argument from a directly parameterized superclass
- **GIVEN** a class `Foo` that extends a generic superclass `Base<String, Integer>`
- **WHEN** `getClassAtIndex<Class<*>>(0)` is called on an instance of `Foo`
- **THEN** the returned type is `String`
- **AND** `getClassAtIndex<Class<*>>(1)` returns `Integer`

#### Scenario: Resolve a type argument across a chain of superclasses
- **GIVEN** a class `Foo` whose generic superclass `Base<String>` itself extends `Root<Long>`
- **WHEN** `getClassAtIndex<Class<*>>(0)` is called on an instance of `Foo`
- **THEN** the returned type is `String`

#### Scenario: Throw when the receiver has no generic superclass
- **GIVEN** a class whose superclass is not parameterized
- **WHEN** `getClassAtIndex<Class<*>>(0)` is called on an instance of that class
- **THEN** a `RuntimeException` is thrown

#### Scenario: Throw when the index is out of bounds
- **GIVEN** a class `Foo` that extends a generic superclass `Base<String>`
- **WHEN** `getClassAtIndex<Class<*>>(2)` is called on an instance of `Foo`
- **THEN** a `RuntimeException` is thrown

### Requirement: newInstance instantiates the class of a reified generic type argument
The extension function `newInstance<T>(index)` SHALL return a new instance of the class resolved at the given generic-superclass index via its no-argument constructor.

#### Scenario: Instantiate from a directly parameterized superclass
- **GIVEN** a class `Foo` that extends a generic superclass `Base<String>` and `String` has a no-argument constructor
- **WHEN** `newInstance<Any>()` is called on an instance of `Foo`
- **THEN** a non-null instance of `String` is returned

#### Scenario: Throw when the resolved class has no no-argument constructor
- **GIVEN** a class `Foo` that extends a generic superclass `Base<StringBuilder>` and `StringBuilder` has no no-argument constructor
- **WHEN** `newInstance<Any>()` is called on an instance of `Foo`
- **THEN** a `RuntimeException` is thrown

### Requirement: asType casts a value to the reified type with a clear failure
The extension function `asType<T : Any>()` SHALL cast the receiver to the reified type `T` when the receiver is assignable to `T`, and throw a `TypeCastException` otherwise.

#### Scenario: Cast a compatible value
- **GIVEN** a value of runtime type `String`
- **WHEN** the value is cast `asType<String>()`
- **THEN** the result is the same value typed as `String`

#### Scenario: Fail on an incompatible value
- **GIVEN** a value of runtime type `String`
- **WHEN** the value is cast `asType<Int>()`
- **THEN** a `TypeCastException` is thrown
