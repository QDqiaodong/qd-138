<template>
  <div class="prop-management">
    <el-card>
      <div class="card-header">
        <el-button type="primary" @click="openAddModal">新增道具</el-button>
        <el-button type="success" @click="openScanDialog">扫码建档</el-button>
        <el-select v-model="filterEra" placeholder="按时代筛选" style="margin-left: 20px; width: 150px;" clearable>
          <el-option v-for="era in eras" :key="era" :label="era" :value="era" />
        </el-select>
        <el-select v-model="filterType" placeholder="按类型筛选" style="margin-left: 10px; width: 150px;" clearable>
          <el-option v-for="type in propTypes" :key="type" :label="type" :value="type" />
        </el-select>
      </div>
      <el-table :data="filteredProps" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="propCode" label="道具编号" width="120" />
        <el-table-column prop="propName" label="道具名称" />
        <el-table-column prop="era" label="适配时代" width="100" />
        <el-table-column prop="propType" label="道具类型" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === '正常' ? 'success' : scope.row.status === '损坏' ? 'warning' : 'danger'">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <el-button size="small" type="primary" @click="openEditModal(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="scanDialogVisible" title="扫码建档" width="520px" @close="stopCamera">
      <div class="scan-area">
        <div v-show="cameraActive" id="prop-scan-reader" class="scan-reader"></div>
        <el-alert
          v-if="cameraError"
          :title="cameraError"
          type="warning"
          :closable="false"
          class="scan-tip"
        />
        <el-alert
          v-else
          title="请将道具编号条码对准摄像头，或使用扫码枪扫入下方输入框"
          type="info"
          :closable="false"
          class="scan-tip"
        />
        <el-input
          ref="scanInputRef"
          v-model="scanInput"
          placeholder="扫不到时可手动录入编号，回车确认"
          clearable
          @keyup.enter="handleManualScanSubmit"
        >
          <template #append>
            <el-button :loading="parsing" @click="handleManualScanSubmit">解析</el-button>
          </template>
        </el-input>
      </div>
    </el-dialog>

    <el-dialog v-model="modalVisible" :title="isEdit ? '编辑道具' : '新增道具'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="道具编号" required>
          <el-input v-model="form.propCode" />
        </el-form-item>
        <el-form-item label="道具名称" required>
          <el-input v-model="form.propName" />
        </el-form-item>
        <el-form-item label="适配时代">
          <el-select v-model="form.era">
            <el-option v-for="era in eras" :key="era" :label="era" :value="era" />
          </el-select>
        </el-form-item>
        <el-form-item label="道具类型">
          <el-select v-model="form.propType">
            <el-option v-for="type in propTypes" :key="type" :label="type" :value="type" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="正常" value="正常" />
            <el-option label="损坏" value="损坏" />
            <el-option label="丢失" value="丢失" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input type="textarea" v-model="form.description" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage, ElMessageBox, type InputInstance } from 'element-plus'
import { Html5Qrcode } from 'html5-qrcode'
import { propApi, getErrorMessage, type Prop, type ScanParseResponse } from '@/api'

const props = ref<Prop[]>([])
const modalVisible = ref(false)
const isEdit = ref(false)
const filterEra = ref<string>('')
const filterType = ref<string>('')
const form = ref<Omit<Prop, 'id' | 'createdAt' | 'updatedAt'>>({
  propCode: '',
  propName: '',
  era: '',
  propType: '',
  description: '',
  status: '正常'
})
const editId = ref<number | null>(null)

const scanDialogVisible = ref(false)
const scanInput = ref('')
const scanInputRef = ref<InputInstance>()
const cameraActive = ref(false)
const cameraError = ref('')
const parsing = ref(false)
let scanner: Html5Qrcode | null = null
let lastScanAttemptAt = 0

const eras = ['民国', '古代', '古风', '仙侠', '未来', '现代', '悬疑']
const propTypes = ['服装', '配饰', '道具', '武器', '乐器', '电子设备', '电子产品', '法器', '丹药', '线索物品']

const filteredProps = computed(() => {
  let result = props.value
  if (filterEra.value) {
    result = result.filter(p => p.era === filterEra.value)
  }
  if (filterType.value) {
    result = result.filter(p => p.propType === filterType.value)
  }
  return result
})

const loadProps = async () => {
  const res = await propApi.getAll()
  props.value = res
}

const openAddModal = () => {
  isEdit.value = false
  editId.value = null
  form.value = { propCode: '', propName: '', era: '', propType: '', description: '', status: '正常' }
  modalVisible.value = true
}

const openScanDialog = async () => {
  scanDialogVisible.value = true
  scanInput.value = ''
  cameraError.value = ''
  await nextTick()
  startCamera()
  scanInputRef.value?.focus()
}

const startCamera = async () => {
  try {
    scanner = new Html5Qrcode('prop-scan-reader')
    await scanner.start(
      { facingMode: 'environment' },
      { fps: 10, qrbox: { width: 280, height: 160 } },
      (decodedText: string) => { handleScannedCode(decodedText) },
      () => { /* 单帧未识别到条码，属正常情况，忽略 */ }
    )
    cameraActive.value = true
  } catch {
    // 无摄像头或未授权时退化为扫码枪/手动录入
    cameraActive.value = false
    cameraError.value = '摄像头不可用，请使用扫码枪扫入或手动录入编号'
    stopCamera()
  }
}

const stopCamera = async () => {
  if (scanner) {
    try {
      if (cameraActive.value) {
        await scanner.stop()
      }
      scanner.clear()
    } catch {
      // 停止失败不影响后续流程
    }
    scanner = null
  }
  cameraActive.value = false
}

const handleManualScanSubmit = () => {
  const code = scanInput.value.trim()
  if (!code) {
    ElMessage.warning('请先扫描或录入道具编号')
    return
  }
  handleScannedCode(code)
}

const handleScannedCode = async (rawCode: string) => {
  const now = Date.now()
  // 摄像头会连续识别同一条码，1.5秒内的重复触发直接忽略
  if (parsing.value || now - lastScanAttemptAt < 1500) {
    return
  }
  lastScanAttemptAt = now
  parsing.value = true
  try {
    const res = await propApi.scanParse(rawCode)
    await handleParsedCode(res)
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '条码解析失败，请重新扫描或手动录入'))
  } finally {
    parsing.value = false
  }
}

const handleParsedCode = async (res: ScanParseResponse) => {
  if (res.exists && res.prop) {
    const existing = res.prop
    await stopCamera()
    scanDialogVisible.value = false
    try {
      await ElMessageBox.confirm(
        `编号「${res.propCode}」已建档：${existing.propName}，是否打开该道具档案？`,
        '该编号已存在',
        { confirmButtonText: '打开档案', cancelButtonText: '继续扫码', type: 'warning' }
      )
      openEditModal(existing)
    } catch {
      openScanDialog()
    }
    return
  }
  await stopCamera()
  scanDialogVisible.value = false
  openAddModal()
  form.value.propCode = res.propCode
  ElMessage.success(`已解析编号「${res.propCode}」，请补全道具信息后建档`)
}

const openEditModal = (row: Prop) => {
  isEdit.value = true
  editId.value = row.id
  form.value = {
    propCode: row.propCode,
    propName: row.propName,
    era: row.era,
    propType: row.propType,
    description: row.description,
    status: row.status
  }
  modalVisible.value = true
}

const handleSubmit = async () => {
  if (!form.value.propCode || !form.value.propName) {
    ElMessage.error('请输入道具编号和名称')
    return
  }
  try {
    if (isEdit.value && editId.value) {
      await propApi.update(editId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await propApi.create(form.value)
      ElMessage.success('创建成功')
    }
    modalVisible.value = false
    loadProps()
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '操作失败'))
  }
}

const handleDelete = async (row: Prop) => {
  try {
    await ElMessageBox.confirm(`确定删除道具「${row.propName}」？`, '提示', { type: 'warning' })
    await propApi.delete(row.id)
    ElMessage.success('删除成功')
    loadProps()
  } catch {
    // cancelled
  }
}

onMounted(loadProps)

onBeforeUnmount(() => {
  stopCamera()
})
</script>

<style scoped>
.card-header {
  margin-bottom: 20px;
}

.scan-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.scan-reader {
  width: 100%;
  min-height: 220px;
  background: #000;
  border-radius: 4px;
  overflow: hidden;
}
</style>