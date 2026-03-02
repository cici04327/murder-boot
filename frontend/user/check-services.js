/**
 * 后端服务健康检查工具
 * 用于诊断服务连接问题
 */

import http from 'http';

const services = [
  { name: '网关 Gateway', port: 8080, path: '/api/user/info' },
  { name: '用户服务 User', port: 8082, path: '/user/info' },
  { name: '门店服务 Store', port: 8083, path: '/store/list' },
  { name: '剧本服务 Script', port: 8084, path: '/script/page' },
  { name: '预约服务 Reservation', port: 8085, path: '/reservation/page' }
];

function checkService(service) {
  return new Promise((resolve) => {
    const options = {
      hostname: 'localhost',
      port: service.port,
      path: service.path || '/',
      method: 'GET',
      timeout: 3000
    };

    const req = http.request(options, (res) => {
      resolve({
        ...service,
        status: 'online',
        statusCode: res.statusCode,
        message: `✓ 服务正常 (HTTP ${res.statusCode})`
      });
    });

    req.on('error', (error) => {
      resolve({
        ...service,
        status: 'offline',
        message: `✗ 服务离线 - ${error.code === 'ECONNREFUSED' ? '连接被拒绝' : error.message}`
      });
    });

    req.on('timeout', () => {
      req.destroy();
      resolve({
        ...service,
        status: 'timeout',
        message: '✗ 服务超时'
      });
    });

    req.end();
  });
}

async function checkAllServices() {
  console.log('=======================================');
  console.log('   剧本杀管理系统 - 服务健康检查');
  console.log('=======================================\n');

  const results = await Promise.all(services.map(checkService));
  
  results.forEach((result) => {
    const statusIcon = result.status === 'online' ? '✓' : '✗';
    const color = result.status === 'online' ? '\x1b[32m' : '\x1b[31m';
    const reset = '\x1b[0m';
    
    console.log(`${color}${statusIcon} [端口 ${result.port}] ${result.name}${reset}`);
    console.log(`  ${result.message}\n`);
  });

  const offlineServices = results.filter(r => r.status !== 'online');
  
  if (offlineServices.length > 0) {
    console.log('\x1b[33m⚠ 警告：以下服务未运行：\x1b[0m');
    offlineServices.forEach(s => {
      console.log(`  - ${s.name} (端口 ${s.port})`);
    });
    console.log('\n\x1b[33m请按以下顺序启动服务：\x1b[0m');
    console.log('  1. 启动 MySQL (端口 3306)');
    console.log('  2. 启动 Redis (端口 6379)');
    console.log('  3. 启动网关: cd murder-gateway && mvn spring-boot:run');
    console.log('  4. 启动用户服务: cd murder-user && mvn spring-boot:run');
    console.log('  5. 启动门店服务: cd murder-store && mvn spring-boot:run');
    console.log('  6. 启动剧本服务: cd murder-script && mvn spring-boot:run');
    console.log('  7. 启动预约服务: cd murder-reservation && mvn spring-boot:run');
    console.log('\n或使用快速检查命令（Windows PowerShell）：');
    console.log('  netstat -ano | findstr "8080 8082 8083 8084 8085"');
    process.exit(1);
  } else {
    console.log('\x1b[32m✓ 所有服务运行正常！\x1b[0m');
    process.exit(0);
  }
}

checkAllServices();

