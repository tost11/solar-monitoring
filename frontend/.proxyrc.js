const {createProxyMiddleware, bodyParser} = require("http-proxy-middleware");

module.exports = function (app) {
  app.use(
      createProxyMiddleware("/api", {
            target: "http://localhost:8080/"
          }
      )
  )
  ;
}