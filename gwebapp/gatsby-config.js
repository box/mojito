module.exports = {
    plugins: [
        {
            resolve: `gatsby-plugin-typescript`,
        },
        'gatsby-plugin-material-ui',
        'gatsby-plugin-react-helmet',
    ],
    proxy: [{
        prefix: "/api",
        url: "http://localhost:8080",
    } ],
}
