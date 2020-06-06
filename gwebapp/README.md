1. Create project with starter

```shell script
nvm use 10.15
gatsby new gwebapp gatsbyjs/gatsby-starter-hello-world
```

1. Add Typescript

https://www.gatsbyjs.org/packages/gatsby-plugin-typescript/

`npm install --save-dev @types/react @types/react-dom @types/node`

Rename `index.js` from the starter into `index.ts`

use plugin with no option for now

```js
  plugins: [{
    resolve: `gatsby-plugin-typescript`,
  },],
```

1. Add very basic client router logic

update `gatsby-node.js`

```javascript
// Implement the Gatsby API “onCreatePage”. This is
// called after every page is created.
exports.onCreatePage = ({ page, actions }) => {
    const { createPage } = actions
    // Make the front page match everything client side.
    // Normally your paths should be a bit more judicious.
    if (page.path === `/`) {
        page.matchPath = `/*`
        createPage(page)
    }
}
```

Add router element, no need for dependencies it is already all there
```jsx
        <nav>
            <Link to="/">Home</Link>
            <Link to="login">Login</Link>
        </nav>
        <Router basepath="/">
            <Home path="/" />
            <Login path="/login"/>
        </Router>
```      

Add types `npm install @types/reach__router --save-dev`   

1. Basic Material UI setup

```json 
    "@material-ui/core": "latest",
    "@material-ui/styles": "latest",
    "@material-ui/icons": "latest",
```

```jsx
    <CssBaseline />
```

1. Redux




1. Brainstorm

* techs
   * local route on first load, in current implementation we're able to load /workbench
   * deep linking (server url --> server gives state, hydrate redux store, navigate to proper page)
   * authentication/re-authentication: going back to a window later on doing edits, save no failing
   * avoid multiple request when backend is slow
   * Search pattern with edits, pagination
   * ids based iterator?
* feature
   * page to review source string a commnicate with devs
   * page to add strings in the tool
       




