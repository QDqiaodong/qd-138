<template>
  <div class="character-review">
    <el-card>
      <div class="card-header">
        <el-date-picker
          v-model="reviewDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择回看日期"
          :clearable="false"
          style="width: 180px;"
          @change="loadReview"
        />
        <el-button @click="loadReview" style="margin-left: 10px;">刷新</el-button>
        <span class="header-tip">回看名单与当天明细同源统计，刷新后保持一致</span>
      </div>

      <div class="stat-row">
        <div class="stat-card">
          <div class="stat-value">{{ review.totalCount }}</div>
          <div class="stat-label">当天变更总数</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ review.roleCount }}</div>
          <div class="stat-label">涉及人物</div>
        </div>
        <div class="stat-card" :class="{ 'stat-warn': review.unconfirmedCount > 0 }">
          <div class="stat-value">{{ review.unconfirmedCount }}</div>
          <div class="stat-label">未确认变更</div>
        </div>
      </div>

      <h3>当天回看名单（{{ review.date }}）</h3>
      <el-table :data="review.summary" border empty-text="当天暂无道具变更">
        <el-table-column prop="roleName" label="人物角色" width="140" />
        <el-table-column prop="themeName" label="所属剧本" width="140" />
        <el-table-column prop="bindCount" label="绑定" width="90" align="center" />
        <el-table-column prop="unbindCount" label="解绑" width="90" align="center" />
        <el-table-column prop="changeCount" label="更换" width="90" align="center" />
        <el-table-column prop="totalCount" label="合计" width="90" align="center" />
        <el-table-column label="未确认变更" width="120" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.unconfirmedCount > 0" type="warning">
              {{ scope.row.unconfirmedCount }} 条未确认
            </el-tag>
            <span v-else>0</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="scope">
            <el-button
              size="small"
              :type="isRoleSelected(scope.row) ? 'primary' : 'default'"
              @click="selectRole(scope.row)"
            >
              {{ isRoleSelected(scope.row) ? '查看全部' : '查看明细' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="detail-header">
        <h3>当天明细</h3>
        <el-tag v-if="selectedRole" type="primary" closable @close="clearRoleFilter">
          仅看：{{ selectedRole.roleName }}
        </el-tag>
      </div>
      <el-table :data="filteredDetails" border empty-text="当天暂无道具变更明细">
        <el-table-column label="变更时间" width="170">
          <template #default="scope">{{ formatTime(scope.row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="propCode" label="道具编号" width="110" />
        <el-table-column prop="propName" label="道具名称" width="120" />
        <el-table-column prop="roleName" label="人物角色" width="110" />
        <el-table-column prop="changeType" label="变更类型" width="90" align="center">
          <template #default="scope">
            <el-tag :type="getChangeTypeTagType(scope.row.changeType)">
              {{ scope.row.changeType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="beforeValue" label="变更前" show-overflow-tooltip />
        <el-table-column prop="afterValue" label="变更后" show-overflow-tooltip />
        <el-table-column label="变更原因" min-width="140" show-overflow-tooltip>
          <template #default="scope">
            <el-tag v-if="scope.row.unconfirmed" type="warning">未确认变更</el-tag>
            <span v-else>{{ scope.row.changeReason }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="100" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { changeRecordApi, type CharacterReviewResponse, type CharacterReviewSummary } from '@/api'

const emptyReview: CharacterReviewResponse = {
  date: '',
  roleCount: 0,
  totalCount: 0,
  unconfirmedCount: 0,
  summary: [],
  details: []
}

const today = () => {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}

const reviewDate = ref(today())
const review = ref<CharacterReviewResponse>({ ...emptyReview })
const selectedRole = ref<CharacterReviewSummary | null>(null)

const getChangeTypeTagType = (type: string) => {
  if (type === '绑定') return 'success'
  if (type === '解绑') return 'danger'
  if (type === '更换') return 'warning'
  return 'info'
}

const formatTime = (time: string) => (time ? time.replace('T', ' ') : '')

const isRoleSelected = (row: CharacterReviewSummary) =>
  selectedRole.value !== null && selectedRole.value.roleId === row.roleId && selectedRole.value.roleName === row.roleName

const filteredDetails = computed(() => {
  if (!selectedRole.value) return review.value.details
  const target = selectedRole.value
  return review.value.details.filter(d => d.roleId === target.roleId && d.roleName === target.roleName)
})

const selectRole = (row: CharacterReviewSummary) => {
  selectedRole.value = isRoleSelected(row) ? null : row
}

const clearRoleFilter = () => {
  selectedRole.value = null
}

// 名单与明细由同一接口返回，一次刷新同时更新，两侧不会出现不一致
const loadReview = async () => {
  const res = await changeRecordApi.getCharacterReview(reviewDate.value || undefined)
  review.value = res || { ...emptyReview }
  // 刷新后若当前筛选的人物已不在回看名单中，清除筛选避免明细为空
  if (selectedRole.value) {
    const target = selectedRole.value
    const stillExists = review.value.summary.some(s => s.roleId === target.roleId && s.roleName === target.roleName)
    if (!stillExists) {
      selectedRole.value = null
    }
  }
}

onMounted(loadReview)
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

.stat-row {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
}

.stat-card {
  flex: 1;
  max-width: 200px;
  padding: 15px;
  background: #fafafa;
  border-radius: 8px;
  text-align: center;
}

.stat-warn {
  background: #fdf6ec;
}

.stat-warn .stat-value {
  color: #e6a23c;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  margin-top: 5px;
  color: #666;
  font-size: 13px;
}

h3 {
  margin: 20px 0 15px;
  color: #333;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.detail-header h3 {
  margin-right: 5px;
}
</style>
