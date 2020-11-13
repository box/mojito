To debug build issue specific to Github actions, use [act](https://github.com/nektos/act).

At the moment of writting `act -v ` doesn't work with Maven. Installing Maven is an option but in the end the issue
showing on Github action was not happening.

To run with the same image as Github action use, `act -v -P ubuntu-latest=nektos/act-environments-ubuntu:18.04`. 
The image is huge though 18G but that might be needed to be able to reproduce issues. Also had to set the locale
manually in the container.

