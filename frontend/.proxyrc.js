const {createProxyMiddleware, bodyParser} = require("http-proxy-middleware");

module.exports = function (app) {

  app.use(
      createProxyMiddleware("/grafana", {
            target: "http://localhost:8080/",
            onProxyRes: function (proxyRes) {
              proxyRes.headers['X-Frame-Options'] = "sameorigin"
            }
          }
      )
  )
  ;

  app.use(
      createProxyMiddleware("/api", {
            target: "http://localhost:8080/"
          }
      )
  )
  ;
}