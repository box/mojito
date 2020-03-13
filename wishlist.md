# Small stuff 
- Get rid of Jadira?
- Get rid of Joda time
- Of old commons-lang?

# Wishlist

- Move from session based authentication to token based (JWT)
- better API/CLI authentication, basically not re-using form login ...  
- clean up rest template - mostly authentication and jackson configuration
- Frontend upgrade or rewrite
    - remove alt, redux?
    - upgrade bootstrap
    - Or rewritte with new stack
        - PWA? 
        - hotreload for dev
        - offline
        - deeplinking
- Introduce the notion of organization
- Authorization
- Translation workbench
- Terminology
- Fuzzy search/matching
- MT connection
- Performance
    - limit msyql usage to bare minimum or remove
    - replace quartz with push/sub or sns/sqs like system
    - replace hibernate listener  with push/sub or sns/sqs like system
- sementic versioning, release not and all
- sla checker sliding windows
    - sla check is not useful right now because no project are fully translated
    - since each dev can track the status for the tranlsation for its branch, maybe that SLA checker is not that useful...

    
# New site
 
Mojito,

A platform to localize your applications, improving translation quality and developper productivity by fully integrating into the CI/CD process.

Don't act after the facts, prevent issues during development. Raise awareness amongst developers by integrating into there day to day
worflow and tools (Github, Phabricator, Slack). Leverage developper to provide screenshots to translator, improving drastically translation quality 
without scalling issues. 

Fully integrated in the development process, don't react to issue after the fact, prevent them during feature work.

Historically localization has been done in parrallel to the development process. Feature are build in English and eventually the get localized. 
A lot of emphasis lately as been put in "continuous localization" but what that really meant is to automate some of the process and speed up
the localization cycle. It is not an integration into the CI/CD cycle. 

Mojito aims at addressing this.  

integrate with Phabricator
integrate with Slack

Want to start something simple, just have a job running in parallel.  

mojito push
mojito pull
