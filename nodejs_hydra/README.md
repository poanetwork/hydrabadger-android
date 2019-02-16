# Firebase SDK for Cloud Functions Quickstart - HTTPS trigger

This function using the **Firebase SDK for Cloud Functions** with an HTTPS to send push to cuurent user.

## Introduction

The function send to `idTo` id push with text `text`

Further reading:

 - [Read more about the Firebase SDK for Cloud Functions](https://firebase.google.com/docs/functions)


## Initial setup, build tools and dependencies

### 1. Clone this repo

Clone or download this repo and open directory.


### 2. Create a Firebase project and configure the quickstart

Create a Firebase Project on the [Firebase Console](https://console.firebase.google.com).

Set up your Firebase project by running `firebase use --add`, select your Project ID and follow the instructions.


### 3. Install the Firebase CLI and enable Functions on your Firebase CLI

You need to have installed the Firebase CLI. If you haven't run:

```bash
npm install -g firebase-tools
```

> Doesn't work? You may need to [change npm permissions](https://docs.npmjs.com/getting-started/fixing-npm-permissions).


## Deploy the app to prod

First you need to install the `npm` dependencies of the functions:

```bash
cd functions && npm install; cd ..
```

This installs locally:
 - The Firebase SDK and the Firebase Functions SDK.
 - The [moment](https://www.npmjs.com/package/moment) npm package to format time.
 - The [cors](https://www.npmjs.com/package/cors) npm package to allow Cross Origin AJAX requests on the endpoint.

Deploy to Firebase using the following command:

```bash
firebase deploy
```

This deploys and activates the date Function.

> The first time you call `firebase deploy` on a new project with Functions will take longer than usual.


## Try the sample

After deploying the function you can open the following URLs in your browser with json params:

```
https://us-central1-<project-id>.cloudfunctions.net/sendPush

```

### License
-----

[![License: LGPL v3.0](https://img.shields.io/badge/License-LGPL%20v3-blue.svg)](https://www.gnu.org/licenses/lgpl-3.0)

This project is licensed under the GNU Lesser General Public License v3.0. See the [LICENSE](LICENSE) file for details.
