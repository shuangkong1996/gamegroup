// utils/config.js
// 环境配置
//
// 若报 net::ERR_SSL_PROTOCOL_ERROR：核对 baseUrl 是否写错域名、本机代理/杀毒是否拦截 HTTPS；
// 开发工具可勾选「不校验合法域名、TLS、HTTPS 证书」；本地联调可改为 http://局域网IP:8080/api。

const env = 'dev'; // dev | prod

const config = {
  dev: {
    // 开发环境：可改为本机后端，例如 'http://192.168.1.10:8080/api'（需配合工具里关闭域名校验）
    baseUrl: 'https://gamegroup.shuangkong.top/api'
  },
  prod: {
    baseUrl: 'https://gamegroup.shuangkong.top/api'
  }
};

module.exports = config[env];
