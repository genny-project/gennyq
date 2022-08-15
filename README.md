# GennyQ

A consolidation of Genny Quarkus services, and necessary dependencies. 

## Building 

```bash
./build.sh;
./build-docker.sh;

#### For individual sub-projects
```bash
./build.sh gadaq;
./build-docker.sh gadaq;
```

## Sub-Projects

### QwandaQ

All Java POJOs, utilities and other globally shared dependency classes.

### ServiceQ

All Service related initialisation code.


### KogitoQ

Generic Kogito related projects.

#### Kogito-Common

Common Kogito service classes and POJOs

#### GadaQ

Main Kogito service for processing of common events.


### Bridge
TODO

### Dropkick
TODO

### Lauchy
TODO

### Messages
TODO

### Shleemy
TODO

### Fyodor
TODO
