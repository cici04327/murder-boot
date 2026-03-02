# 文件上传目录

该目录用于存储用户上传的图片文件。

## 目录结构

```
uploads/
├── images/          # 通用图片（门店图片等）
│   └── 2026/
│       └── 01/
│           └── 04/
│               └── xxx.jpg
├── scripts/         # 剧本相关
│   └── covers/      # 剧本封面
│       └── 2026/
│           └── 01/
│               └── 04/
│                   └── xxx.jpg
└── README.md
```

## 访问方式

上传成功后，图片可通过以下 URL 访问：
```
http://localhost:8001/upload/images/2026/01/04/xxx.jpg
http://localhost:8001/upload/scripts/covers/2026/01/04/xxx.jpg
```

## 注意事项

1. 文件会按照 `年/月/日` 的目录结构自动存储
2. 文件名使用 UUID 生成，避免重复
3. 支持的图片格式：jpg、png、gif 等
4. 单个文件大小限制：5MB
5. 批量上传最多：10张
