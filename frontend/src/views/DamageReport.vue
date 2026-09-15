<template>
  <div class="damage-report">
    <el-card>
      <div class="card-header">
        <el-button type="primary" @click="openCreateDialog">开报损单</el-button>
        <el-button @click="loadAll" style="margin-left: 10px;">刷新</el-button>
        <el-select v-model="filterStatus" placeholder="按状态筛选" style="margin-left: 10px; width: 140px;" clearable>
          <el-option label="未结案" value="未结案" />
          <el-option label="已结案" value="已结案" />
        </el-select>
        <span class="header-tip">同一道具存在未结案报损单时不能再开新单，结案后才可重开</span>
      </div>

      <el-table :data="filteredReports" border empty-text="暂无报损单">
        <el-table-column prop="reportNo" label="报损单号" width="110" />
        <el-table-column prop="propCode" label="道具编号" width="110" />
        <el-table-column prop="propName" label="道具名称" width="130" />
        <el-table-column prop="damagedPart" label="损坏部位" min-width="130" show-overflow-tooltip />
        <el-table-column prop="discoverer" label="发现人" width="110" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === '未结案' ? 'danger' : 'success'">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开单时间" width="170">
          <template #default="scope">{{ formatTime(scope.row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="结案时间" width="170">
          <template #default="scope">{{ scope.row.closedAt ? formatTime(scope.row.closedAt) : '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center">
          <template #default="scope">
            <el-button size="small" type="primary" @click="openDetail(scope.row)">查看</el-button>
            <el-button
              v-if="scope.row.status === '未结案'"
              size="small"
              type="warning"
              @click="handleClose(scope.row)"
            >结案</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="createVisible" title="开报损单" width="500px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="报损道具" required>
          <el-select
            v-model="createForm.propId"
            placeholder="选择现有道具"
            filterable
            style="width: 100%;"
          >
            <el-option
              v-for="prop in props"
              :key="prop.id"
              :label="`${prop.propCode} ${prop.propName}`"
              :value="prop.id"
              :disabled="openReportPropIds.has(prop.id)"
            >
              <span>{{ prop.propCode }} {{ prop.propName }}</span>
              <span v-if="openReportPropIds.has(prop.id)" class="option-tip">已有未结案报损单</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="损坏部位" required>
          <el-input
            v-model="createForm.damagedPart"
            placeholder="如：剑柄缠绳断裂处、礼帽帽檐"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="发现人" required>
          <el-input
            v-model="createForm.discoverer"
            placeholder="发现道具损坏的场务/人员姓名"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">开单</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" :title="`报损单 ${detail?.reportNo ?? ''}`" width="500px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="报损单号">{{ detail.reportNo }}</el-descriptions-item>
        <el-descriptions-item label="报损道具">
          {{ detail.propCode }} {{ detail.propName }}
        </el-descriptions-item>
        <el-descriptions-item label="损坏部位">
          <span class="original-text">{{ detail.damagedPart }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="发现人">
          <span class="original-text">{{ detail.discoverer }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === '未结案' ? 'danger' : 'success'">{{ detail.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="开单时间">{{ formatTime(detail.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="结案时间">
          {{ detail.closedAt ? formatTime(detail.closedAt) : '—' }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="detail && detail.status === '未结案'"
          type="warning"
          @click="handleClose(detail)"
        >结案</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { damageReportApi, propApi, getErrorMessage, type DamageReport, type Prop } from '@/api'

const reports = ref<DamageReport[]>([])
const props = ref<Prop[]>([])
const filterStatus = ref<string>('')

const createVisible = ref(false)
const submitting = ref(false)
const createForm = ref<{ propId: number | null; damagedPart: string; discoverer: string }>({
  propId: null,
  damagedPart: '',
  discoverer: ''
})

const detailVisible = ref(false)
const detail = ref<DamageReport | null>(null)

// 有未结案报损单的道具不允许再开新单，下拉中直接置灰提示
const openReportPropIds = computed(() => {
  const ids = new Set<number>()
  for (const report of reports.value) {
    if (report.status === '未结案') {
      ids.add(report.propId)
    }
  }
  return ids
})

const filteredReports = computed(() => {
  if (!filterStatus.value) return reports.value
  return reports.value.filter(r => r.status === filterStatus.value)
})

const formatTime = (time: string) => (time ? time.replace('T', ' ') : '')

const loadAll = async () => {
  const [reportList, propList] = await Promise.all([damageReportApi.getAll(), propApi.getAll()])
  reports.value = reportList
  props.value = propList
}

const openCreateDialog = () => {
  createForm.value = { propId: null, damagedPart: '', discoverer: '' }
  createVisible.value = true
}

const handleCreate = async () => {
  if (!createForm.value.propId) {
    ElMessage.warning('请选择要报损的道具')
    return
  }
  if (!createForm.value.damagedPart.trim()) {
    ElMessage.warning('请填写损坏部位')
    return
  }
  if (!createForm.value.discoverer.trim()) {
    ElMessage.warning('请填写发现人')
    return
  }
  submitting.value = true
  try {
    await damageReportApi.create({
      propId: createForm.value.propId,
      damagedPart: createForm.value.damagedPart.trim(),
      discoverer: createForm.value.discoverer.trim()
    })
    ElMessage.success('报损单已开立，道具档案状态已同步为「损坏」')
    createVisible.value = false
    loadAll()
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '开单失败，请重试'))
  } finally {
    submitting.value = false
  }
}

// 打开报损单时按单号重新取详情，损坏部位与发现人始终展示开单录入的原文
const openDetail = async (row: DamageReport) => {
  try {
    detail.value = await damageReportApi.getById(row.id)
    detailVisible.value = true
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '报损单打开失败，请刷新后重试'))
  }
}

const handleClose = async (row: DamageReport) => {
  try {
    await ElMessageBox.confirm(
      `确定将报损单「${row.reportNo}」（${row.propName}）结案？结案后该道具可再开新单。`,
      '结案确认',
      { confirmButtonText: '结案', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await damageReportApi.close(row.id)
    ElMessage.success('已结案')
    detailVisible.value = false
    loadAll()
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '结案失败，请重试'))
  }
}

onMounted(loadAll)
</script>

<style scoped>
.card-header {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.header-tip {
  margin-left: 15px;
  color: #999;
  font-size: 13px;
}

.option-tip {
  float: right;
  color: #e6a23c;
  font-size: 12px;
}

.original-text {
  font-weight: bold;
  color: #333;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
