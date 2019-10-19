GlobalInfo
==========

GlobalInfo is a collection of Java utilities for working with global information sharing.

`RoutingMap`
------------

The `RoutingMap` class exposes a global set of key-value pairs that route to specified targets within the application. There is to be a single `RoutingMap` in any given program. Extreme care must be used to ensure that there are no name clashes. The `register` method is used to add routes to the map, using an implementation of the `Source` interface. There are two predefined implementations: one that uses a `MethodHandle` to retrieve the information, and one that allows the application to choose which source to route to, mostly useful for debugging.

Contact
-------

The author can be contacted at [<circuitcr4ft@gmail.com>](mailto:circuitcr4ft@gmail.com)