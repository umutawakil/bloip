# Overview
Audio based social network (Tweets but with short audio recordings). The recordings take place in JavaScript on the front-end of the web application or on Android mobile apps and are then uploaded directly to an s3 bucket fronted by a CDN (CloudFront). Background jobs will then convert the files to the appropriate format and not every browser records to the ideal .m4a format. Both the web application and mobile app try to confine content to the language and country of the user making the submission.

# Components
The is project contains all of the web application logic and infrastructure code. For the mobile see bloip-android.

# General Notes
Most of this project tries to apply Active record style true object oriented programming to a spring project so it doesn't use anemic domain objects but rather business logic is actually contained in the domain objects.

# Future Improvements
The future hope is to abstract the union code that makes this possible with Spring. Right now its a bit messy in getting, for example, a User to save itself by statically calling upon it's own userRepository.
