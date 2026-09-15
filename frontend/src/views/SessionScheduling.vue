<template>
  <div class="session-scheduling">
    <el-card>
      <div class="card-header">
        <el-button type="primary" @click="openCreateDialog">开一场</el-button>
        <el-button @click="openPerformerDialog" style="margin-left: 10px;">人员名册</el-button>
        <el-button @click="loadSessions" style="margin-left: 10px;">刷新</el-button>
        <el-select v-model="filterStatus" placeholder="按状态筛选" style="margin-left: 10px; width: 140px;" clearable>
          <el-option label="排班中" value="排班中" />
          <el-option label="已排好" value="已排好" />
          <el-option label="开演中" value="开演中" />
        </el-select>
        <span class="header-tip">主题里的人物全部排完才能标为已排好；同一个人时间重叠的场次会被拦住；有待换的场次开演会被拦下</span>
      </div>

      <el-table :data="filteredSessions" border empty-text="暂无场次，点击「开一场」开始排班">
        <el-table-column prop="sessionNo" label="场次编号" width="110" />
        <el-table-column prop="themeName" label="演出主题" min-width="120" />
        <el-table-column label="开演时间" width="160">
          <template #default="scope">{{ formatTime(scope.row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="160">
          <template #default="scope">{{ formatTime(scope.row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="排班进度" width="110" align="center">
          <template #default="scope">
            <span :class="{ 'progress-done': scope.row.assignedCount >= scope.row.totalRoles && scope.row.totalRoles > 0 }">
              {{ scope.row.assignedCount }}/{{ scope.row.totalRoles }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="130" align="center">
          <template #default="scope">
            <el-tag :type="statusTagType(scope.row.status)">
              {{ scope.row.status }}
            </el-tag>
            <el-tag
              v-if="scope.row.pendingReplacementCount > 0"
              type="danger"
              size="small"
              style="margin-left: 4px;"
            >待换 {{ scope.row.pendingReplacementCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" align="center">
          <template #default="scope">
            <el-button size="small" type="primary" @click="openScheduleDialog(scope.row)">排班</el-button>
            <el-button
              v-if="scope.row.status === '排班中'"
              size="small"
              type="success"
              :disabled="scope.row.totalRoles === 0 || scope.row.assignedCount < scope.row.totalRoles"
              @click="handleReady(scope.row)"
            >标为已排好</el-button>
            <el-button
              v-if="scope.row.status === '已排好'"
              size="small"
              type="danger"
              @click="handleStart(scope.row)"
            >开演</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="createVisible" title="开一场" width="480px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="演出主题" required>
          <el-select v-model="createForm.scriptThemeId" placeholder="选择本场演出的剧本主题" style="width: 100%;">
            <el-option
              v-for="theme in themes"
              :key="theme.id"
              :label="theme.themeName"
              :value="theme.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="开演时间" required>
          <el-date-picker
            v-model="createForm.startTime"
            type="datetime"
            placeholder="选择开演时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-date-picker
            v-model="createForm.endTime"
            type="datetime"
            placeholder="选择结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%;"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">开场</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="scheduleVisible"
      :title="currentSession ? `排班 - ${currentSession.sessionNo} ${currentSession.themeName}` : '排班'"
      width="640px"
    >
      <template v-if="currentSession">
        <div class="schedule-meta">
          <span>开演：{{ formatTime(currentSession.startTime) }} ~ {{ formatTime(currentSession.endTime) }}</span>
          <span class="schedule-progress">
            已排 {{ currentSession.assignedCount }}/{{ currentSession.totalRoles }}
            <el-tag :type="statusTagType(currentSession.status)" size="small" style="margin-left: 8px;">
              {{ currentSession.status }}
            </el-tag>
            <el-tag
              v-if="currentSession.pendingReplacementCount > 0"
              type="danger"
              size="small"
              style="margin-left: 4px;"
            >待换 {{ currentSession.pendingReplacementCount }}</el-tag>
          </span>
        </div>
        <el-table :data="scheduleRows" border empty-text="该主题下还没有人物，请先到人物角色管理添加">
          <el-table-column prop="roleName" label="人物" width="140" />
          <el-table-column label="演职人员" min-width="200">
            <template #default="scope">
              <el-select
                :model-value="assignMap[scope.row.id] ?? null"
                placeholder="选择演职人员"
                clearable
                style="width: 100%;"
                @update:model-value="(val: number | null) => handleAssign(scope.row.id, val)"
              >
                <el-option
                  v-for="performer in performers"
                  :key="performer.id"
                  :label="performer.performerName + (performer.suspended ? '（停演）' : '')"
                  :value="performer.id"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="180" align="center">
            <template #default="scope">
              <template v-if="assignmentViewMap[scope.row.id]?.pendingReplacement">
                <el-tag type="danger" size="small">待换</el-tag>
                <div class="suspend-reason">停演原因：{{ assignmentViewMap[scope.row.id].suspendReason }}</div>
              </template>
              <span v-else-if="assignMap[scope.row.id]" class="assigned-ok">已排</span>
              <span v-else class="assigned-none">未排</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" align="center">
            <template #default="scope">
              <el-button
                v-if="assignMap[scope.row.id]"
                size="small"
                type="danger"
                link
                @click="handleUnassign(scope.row.id)"
              >撤下</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="schedule-tip">换演员直接重新选择即可，换完刷新仍是新的人；时间重叠会提示撞上了哪一场；标了待换的行要换掉才能开演</div>
      </template>
      <template #footer>
        <el-button
          v-if="currentSession && currentSession.status === '排班中'"
          type="success"
          :disabled="currentSession.totalRoles === 0 || currentSession.assignedCount < currentSession.totalRoles"
          @click="handleReady(currentSession)"
        >标为已排好</el-button>
        <el-button
          v-if="currentSession && currentSession.status === '已排好'"
          type="danger"
          @click="handleStart(currentSession)"
        >开演</el-button>
        <el-button @click="scheduleVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="performerVisible" title="人员名册" width="640px">
      <div class="performer-add">
        <el-input
          v-model="newPerformerName"
          placeholder="输入演职人员姓名"
          maxlength="100"
          style="flex: 1;"
          @keyup.enter="handleAddPerformer"
        />
        <el-button type="primary" :loading="performerSubmitting" @click="handleAddPerformer" style="margin-left: 10px;">
          添加
        </el-button>
      </div>
      <el-table :data="performers" border empty-text="名册为空，先添加演职人员">
        <el-table-column prop="performerName" label="姓名" min-width="100" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.suspended ? 'danger' : 'success'" size="small">
              {{ scope.row.suspended ? '停演' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="停演原因" min-width="140">
          <template #default="scope">
            <span v-if="scope.row.suspended">{{ scope.row.suspendReason }}</span>
            <span v-else class="assigned-none">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="scope">
            <el-button
              v-if="!scope.row.suspended"
              size="small"
              type="warning"
              link
              @click="handleSuspend(scope.row)"
            >标停演</el-button>
            <el-button
              v-else
              size="small"
              type="success"
              link
              @click="handleResume(scope.row)"
            >恢复演出</el-button>
            <el-button size="small" type="danger" link @click="handleDeletePerformer(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="schedule-tip">标停演后，未开演场次的名单里此人会标成待换，有待换的场次开演会被拦下</div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  sessionApi, performerApi, themeApi, roleApi, getErrorMessage,
  type ShowSession, type Performer, type ScriptTheme, type CharacterRole, type SessionAssignmentView
} from '@/api'

const sessions = ref<ShowSession[]>([])
const performers = ref<Performer[]>([])
const themes = ref<ScriptTheme[]>([])
const filterStatus = ref<string>('')

const createVisible = ref(false)
const submitting = ref(false)
const createForm = ref<{ scriptThemeId: number | null; startTime: string; endTime: string }>({
  scriptThemeId: null,
  startTime: '',
  endTime: ''
})

const scheduleVisible = ref(false)
const currentSession = ref<ShowSession | null>(null)
const themeRoles = ref<CharacterRole[]>([])
/** 人物ID -> 演职人员ID，以服务端返回的排班为准，操作失败会重新拉取覆盖 */
const assignMap = ref<Record<number, number | null>>({})

const performerVisible = ref(false)
const newPerformerName = ref('')
const performerSubmitting = ref(false)

const filteredSessions = computed(() => {
  if (!filterStatus.value) return sessions.value
  return sessions.value.filter(s => s.status === filterStatus.value)
})

const scheduleRows = computed(() => themeRoles.value)

/** 人物ID -> 排班视图（含待换标记与停演原因），以服务端返回为准 */
const assignmentViewMap = computed<Record<number, SessionAssignmentView>>(() => {
  const map: Record<number, SessionAssignmentView> = {}
  for (const a of currentSession.value?.assignments ?? []) {
    map[a.characterRoleId] = a
  }
  return map
})

const statusTagType = (status: string): 'success' | 'warning' | 'info' => {
  if (status === '已排好') return 'success'
  if (status === '开演中') return 'info'
  return 'warning'
}

const formatTime = (time: string) => (time ? time.replace('T', ' ').slice(0, 16) : '')

const loadSessions = async () => {
  sessions.value = await sessionApi.getAll()
}

const loadBase = async () => {
  const [performerList, themeList] = await Promise.all([performerApi.getAll(), themeApi.getAll()])
  performers.value = performerList
  themes.value = themeList
}

const openCreateDialog = () => {
  createForm.value = { scriptThemeId: null, startTime: '', endTime: '' }
  createVisible.value = true
}

const handleCreate = async () => {
  if (!createForm.value.scriptThemeId) {
    ElMessage.warning('请选择本场演出的剧本主题')
    return
  }
  if (!createForm.value.startTime || !createForm.value.endTime) {
    ElMessage.warning('请填写本场开演和结束时间')
    return
  }
  submitting.value = true
  try {
    await sessionApi.create({
      scriptThemeId: createForm.value.scriptThemeId,
      startTime: createForm.value.startTime,
      endTime: createForm.value.endTime
    })
    ElMessage.success('场次已开立，请继续排班')
    createVisible.value = false
    loadSessions()
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '开场失败，请重试'))
  } finally {
    submitting.value = false
  }
}

// 打开排班时按场次ID重新取详情，排班情况始终与服务端一致
const openScheduleDialog = async (row: ShowSession) => {
  try {
    const [detail, roles] = await Promise.all([
      sessionApi.getById(row.id),
      roleApi.getByTheme(row.scriptThemeId)
    ])
    currentSession.value = detail
    themeRoles.value = roles
    syncAssignMap(detail)
    scheduleVisible.value = true
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '场次打开失败，请刷新后重试'))
  }
}

const syncAssignMap = (detail: ShowSession) => {
  const map: Record<number, number | null> = {}
  for (const role of themeRoles.value) {
    map[role.id] = null
  }
  for (const assignment of detail.assignments) {
    map[assignment.characterRoleId] = assignment.performerId
  }
  assignMap.value = map
}

// 选人即排班；该人物已排过人时后端按换人处理，换完重新拉取仍是新的人
const handleAssign = async (roleId: number, performerId: number | null) => {
  if (!currentSession.value) return
  // 清空选择（可能回传 null/undefined/空串）等同撤下
  if (!performerId) {
    if (assignMap.value[roleId]) {
      await handleUnassign(roleId)
    }
    return
  }
  try {
    const detail = await sessionApi.assign(currentSession.value.id, {
      characterRoleId: roleId,
      performerId
    })
    currentSession.value = detail
    syncAssignMap(detail)
    loadSessions()
  } catch (err) {
    // 同场重复、时间重叠等被后端拦住时，提示里会写明撞上的场次，并重新拉取恢复原选择
    ElMessage.error(getErrorMessage(err, '排班失败，请重试'))
    const detail = await sessionApi.getById(currentSession.value.id)
    currentSession.value = detail
    syncAssignMap(detail)
  }
}

const handleUnassign = async (roleId: number) => {
  if (!currentSession.value) return
  try {
    const detail = await sessionApi.unassign(currentSession.value.id, roleId)
    currentSession.value = detail
    syncAssignMap(detail)
    loadSessions()
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '撤下失败，请重试'))
  }
}

const handleReady = async (row: ShowSession) => {
  try {
    await ElMessageBox.confirm(
      `确定场次「${row.sessionNo} ${row.themeName}」人物已全部排好？`,
      '标为已排好',
      { confirmButtonText: '标为已排好', cancelButtonText: '取消', type: 'success' }
    )
  } catch {
    return
  }
  try {
    await sessionApi.markReady(row.id)
    ElMessage.success('已标为已排好')
    scheduleVisible.value = false
    loadSessions()
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '操作失败，请重试'))
  }
}

// 点开演：只要这场还有待换，后端就会拦下并写出停演演员、所演人物和原因原文
const handleStart = async (row: ShowSession) => {
  try {
    await ElMessageBox.confirm(
      `确定场次「${row.sessionNo} ${row.themeName}」现在开演？开演后转为开演中。`,
      '开演确认',
      { confirmButtonText: '开演', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    const detail = await sessionApi.start(row.id)
    ElMessage.success(`场次「${row.sessionNo}」已开演`)
    if (currentSession.value && currentSession.value.id === row.id) {
      currentSession.value = detail
      syncAssignMap(detail)
    }
    loadSessions()
  } catch (err) {
    // 被拦时弹出待换明细：停演演员名、所演人物、停演原因原文
    ElMessageBox.alert(getErrorMessage(err, '开演失败，请重试'), '开演被拦下', {
      confirmButtonText: '知道了',
      type: 'error'
    })
    loadSessions()
    if (currentSession.value && currentSession.value.id === row.id) {
      const detail = await sessionApi.getById(row.id)
      currentSession.value = detail
      syncAssignMap(detail)
    }
  }
}

// 标停演：必填停演原因，标停后未开演场次的名单里此人标为待换
const handleSuspend = async (row: Performer) => {
  let reason: string
  try {
    const { value } = await ElMessageBox.prompt(
      `请填写「${row.performerName}」的停演原因（将原样写在开演拦截提示里）`,
      '标停演',
      {
        confirmButtonText: '标停演',
        cancelButtonText: '取消',
        type: 'warning',
        inputPlaceholder: '例如：突发高烧，临时无法上场',
        inputValidator: (val: string) => (val && val.trim() ? true : '请填写停演原因')
      }
    )
    reason = value.trim()
  } catch {
    return
  }
  try {
    await performerApi.suspend(row.id, reason)
    ElMessage.success(`已标停演：「${row.performerName}」`)
    await refreshAfterPerformerChange()
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '标停演失败，请重试'))
  }
}

const handleResume = async (row: Performer) => {
  try {
    await ElMessageBox.confirm(
      `确定「${row.performerName}」恢复演出？各未开演场次的待换标记随即消失。`,
      '恢复演出',
      { confirmButtonText: '恢复演出', cancelButtonText: '取消', type: 'success' }
    )
  } catch {
    return
  }
  try {
    await performerApi.resume(row.id)
    ElMessage.success(`已恢复演出：「${row.performerName}」`)
    await refreshAfterPerformerChange()
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '恢复演出失败，请重试'))
  }
}

// 停演状态变了，名册、场次列表和正开着的排班对话框都要重新拉，待换标记才一致
const refreshAfterPerformerChange = async () => {
  await Promise.all([loadBase(), loadSessions()])
  if (scheduleVisible.value && currentSession.value) {
    const detail = await sessionApi.getById(currentSession.value.id)
    currentSession.value = detail
    syncAssignMap(detail)
  }
}

const openPerformerDialog = () => {
  newPerformerName.value = ''
  performerVisible.value = true
}

const handleAddPerformer = async () => {
  const name = newPerformerName.value.trim()
  if (!name) {
    ElMessage.warning('请填写演职人员姓名')
    return
  }
  performerSubmitting.value = true
  try {
    await performerApi.create({ performerName: name })
    ElMessage.success(`已添加「${name}」`)
    newPerformerName.value = ''
    loadBase()
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '添加失败，请重试'))
  } finally {
    performerSubmitting.value = false
  }
}

const handleDeletePerformer = async (row: Performer) => {
  try {
    await ElMessageBox.confirm(
      `确定把「${row.performerName}」从名册删除？有排班在身的人员无法删除。`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await performerApi.delete(row.id)
    ElMessage.success('已删除')
    loadBase()
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '删除失败，请重试'))
  }
}

onMounted(() => {
  loadSessions()
  loadBase()
})
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

.progress-done {
  color: #67c23a;
  font-weight: bold;
}

.schedule-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  color: #666;
  font-size: 14px;
}

.schedule-tip {
  margin-top: 12px;
  color: #999;
  font-size: 13px;
}

.suspend-reason {
  margin-top: 4px;
  color: #f56c6c;
  font-size: 12px;
  line-height: 1.4;
}

.assigned-ok {
  color: #67c23a;
  font-size: 13px;
}

.assigned-none {
  color: #c0c4cc;
  font-size: 13px;
}

.performer-add {
  display: flex;
  margin-bottom: 15px;
}
</style>
