# Overview
Audio based social network (Tweets but with short audio recordings)

# Components
The is project contains all of the web application logic and infrastructure code. For the mobile see bloip-android.

# General Notes
Most of this project tries to apply Active record style true object oriented programming to a spring project so it doesn't use anemic domain objects but rather business logic is actually contained in the domain objects.

# Future Improvements
The future hope is to abstract the union code that makes this possible with Spring. Right now its a bit messy in getting, for example, a User to save itself by statically calling upon it's own userRepository.
