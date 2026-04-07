// check-images.js
// 检查图片文件大小，找出需要删除的大文件

const fs = require('fs');
const path = require('path');

const imagesDir = path.join(__dirname, 'images');
const maxSize = 200 * 1024; // 200KB，超过这个大小的文件建议删除

// 需要保留的文件（tabBar图标等小文件）
const keepFiles = [
  'tab-home.png',
  'tab-home-active.png',
  'tab-profile.png',
  'tab-profile-active.png',
  'avatar-default.png', // 需要上传到服务器，但可以先保留
  'game-default.png'     // 需要上传到服务器，但可以先保留
];

// 需要上传到服务器后删除的文件
const uploadThenDelete = [
  'avatar-default.png',
  'game-default.png'
];

// 可以直接删除的大文件
const deleteFiles = [
  'game-chengyu.png',
  'game-stone.png',
  'game-number.png',
  'logo.png'
];

console.log('📦 小程序图片文件检查工具\n');
console.log('=' .repeat(50));

if (!fs.existsSync(imagesDir)) {
  console.error('❌ images 目录不存在！');
  process.exit(1);
}

const files = fs.readdirSync(imagesDir);
const imageFiles = files.filter(file => 
  /\.(png|jpg|jpeg|gif|webp)$/i.test(file)
);

if (imageFiles.length === 0) {
  console.log('✅ images 目录中没有图片文件');
  process.exit(0);
}

console.log(`\n📁 找到 ${imageFiles.length} 个图片文件\n`);

let totalSize = 0;
const fileInfo = [];

// 检查每个文件
imageFiles.forEach(file => {
  const filePath = path.join(imagesDir, file);
  const stats = fs.statSync(filePath);
  const size = stats.size;
  const sizeKB = (size / 1024).toFixed(2);
  const sizeMB = (size / 1024 / 1024).toFixed(2);
  
  totalSize += size;
  
  fileInfo.push({
    name: file,
    size: size,
    sizeKB: parseFloat(sizeKB),
    sizeMB: parseFloat(sizeMB),
    path: filePath
  });
});

// 按大小排序
fileInfo.sort((a, b) => b.size - a.size);

// 显示文件信息
console.log('📊 文件大小统计：\n');
fileInfo.forEach((file, index) => {
  const sizeStr = file.sizeMB > 1 
    ? `${file.sizeMB} MB` 
    : `${file.sizeKB} KB`;
  
  const status = getFileStatus(file.name, file.size);
  const icon = status === 'keep' ? '✅' : status === 'upload' ? '📤' : '❌';
  
  console.log(`${icon} ${(index + 1).toString().padStart(2, ' ')}. ${file.name.padEnd(25)} ${sizeStr.padStart(10)} ${status}`);
});

// 计算总大小
const totalKB = (totalSize / 1024).toFixed(2);
const totalMB = (totalSize / 1024 / 1024).toFixed(2);

console.log('\n' + '='.repeat(50));
console.log(`📦 总大小: ${totalMB > 1 ? totalMB + ' MB' : totalKB + ' KB'}`);
console.log(`⚠️  小程序限制: 2 MB (主包)`);

if (totalSize > 2 * 1024 * 1024) {
  console.log(`\n❌ 总大小超过限制！需要删除 ${((totalSize - 2 * 1024 * 1024) / 1024 / 1024).toFixed(2)} MB`);
} else {
  console.log(`\n✅ 总大小在限制范围内`);
}

// 分析建议
console.log('\n' + '='.repeat(50));
console.log('💡 操作建议：\n');

const toDelete = fileInfo.filter(f => 
  !keepFiles.includes(f.name) && 
  (f.size > maxSize || deleteFiles.includes(f.name))
);

const toUpload = fileInfo.filter(f => 
  uploadThenDelete.includes(f.name)
);

const toKeep = fileInfo.filter(f => 
  keepFiles.includes(f.name) && !uploadThenDelete.includes(f.name)
);

if (toDelete.length > 0) {
  console.log('❌ 可以直接删除的文件（大文件或不需要的文件）：');
  let deleteSize = 0;
  toDelete.forEach(file => {
    console.log(`   - ${file.name} (${file.sizeMB > 1 ? file.sizeMB + ' MB' : file.sizeKB + ' KB'})`);
    deleteSize += file.size;
  });
  console.log(`   删除后可节省: ${(deleteSize / 1024 / 1024).toFixed(2)} MB\n`);
}

if (toUpload.length > 0) {
  console.log('📤 需要上传到服务器后删除的文件：');
  let uploadSize = 0;
  toUpload.forEach(file => {
    console.log(`   - ${file.name} (${file.sizeMB > 1 ? file.sizeMB + ' MB' : file.sizeKB + ' KB'})`);
    uploadSize += file.size;
  });
  console.log(`   上传后可节省: ${(uploadSize / 1024 / 1024).toFixed(2)} MB\n`);
  console.log('   上传路径: /home/dr/docker/gamegroup/images/\n');
}

if (toKeep.length > 0) {
  console.log('✅ 需要保留的文件（tabBar图标等）：');
  toKeep.forEach(file => {
    console.log(`   - ${file.name} (${file.sizeMB > 1 ? file.sizeMB + ' MB' : file.sizeKB + ' KB'})`);
  });
  console.log('');
}

// 生成删除命令
if (toDelete.length > 0 || toUpload.length > 0) {
  console.log('='.repeat(50));
  console.log('📝 删除命令（复制执行）：\n');
  
  const allToDelete = [...toDelete, ...toUpload];
  allToDelete.forEach(file => {
    console.log(`rm "${file.path}"`);
  });
  
  console.log('\n或者批量删除：');
  const fileNames = allToDelete.map(f => f.name).join(' ');
  console.log(`cd ${imagesDir} && rm ${fileNames}`);
}

function getFileStatus(fileName, fileSize) {
  if (keepFiles.includes(fileName) && !uploadThenDelete.includes(fileName)) {
    return 'keep';
  }
  if (uploadThenDelete.includes(fileName)) {
    return 'upload';
  }
  if (fileSize > maxSize || deleteFiles.includes(fileName)) {
    return 'delete';
  }
  return 'check';
}

