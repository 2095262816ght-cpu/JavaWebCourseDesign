import { ElMessage, type UploadProps } from 'element-plus'

/** 允许上传的图片后缀（显式排除 SVG，避免脚本注入风险） */
const ALLOWED_IMAGE_EXTS = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp', '.ico']
const MAX_IMAGE_SIZE_MB = 5

/**
 * 图片上传前的统一校验（类型白名单 + 体积上限），供品牌 Logo 与商品主图/副图共用
 */
export const beforeImageUpload: UploadProps['beforeUpload'] = (file) => {
  const lowerName = (file.name || '').toLowerCase()
  const lowerType = (file.type || '').toLowerCase()
  const isSvg = lowerName.endsWith('.svg') || lowerType.includes('svg')
  const hasAllowedExt = ALLOWED_IMAGE_EXTS.some((ext) => lowerName.endsWith(ext))
  const isImageMime = lowerType.startsWith('image/') && !isSvg
  const isImage = !isSvg && (isImageMime || (!lowerType && hasAllowedExt)) && (hasAllowedExt || !lowerName.includes('.'))
  const isLt5M = file.size / 1024 / 1024 < MAX_IMAGE_SIZE_MB

  if (!isImage) {
    ElMessage.error('只允许上传图片文件 (JPG/PNG/GIF/WebP/BMP/ICO)')
    return false
  }
  if (!isLt5M) {
    ElMessage.error(`上传图片大小不能超过 ${MAX_IMAGE_SIZE_MB}MB`)
    return false
  }
  return true
}
