# applicationLifecycle

## Overview
This lifecycle/workflow governs how the cards move through the process view. This is the engine room of the Genny system and provides the smarts and our point of difference with other software.

## Structure
There is 4 sub-processes that represent the 4 buckets (Applied, Shortlisted, Interview, Offered). At the end of the process the placementLifecycle is signalled to continue with the process view buckets (Placed and In Progress). The Available bucket is not represented here as there is no direct link between the Available and Applied buckets.

## How it works
* A `token` is generated with the newApplication Signal which is a HashMap.
* The `token` enters the applicationLifecycle Box and 
* The first time a bucket is reached, the required messages are triggered to be sent by the notificationHub.
* The token then reaches a 