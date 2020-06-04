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

2.