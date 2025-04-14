const {createProxyMiddleware, bodyParser} = require("http-proxy-middleware");

module.exports = function (app) {
  app.use(
      createProxyMiddleware("/api", {
            target: "http://localhost:8050/"
          }
      )
  )
  app.use(
    createProxyMiddleware("/oauth2", {
        target: "http://localhost:8050/"
      }
    )
  )
  ;
}
