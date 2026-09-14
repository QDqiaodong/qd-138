<template>
  <div class="association-management">
    <el-card>
      <div class="card-header">
        <el-button type="primary" @click="openBindModal">绑定道具</el-button>
        <el-button type="warning" @click="openChangeModal">更换道具角色</el-button>
      </div>
      <el-table :data="roleProps" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="themeName" label="剧本主题" width="120" />
        <el-table-column prop="roleName" label="角色名称" width="120" />
        <el-table-column prop="propCode" label="道具编号" width="120" />
        <el-table-column prop="propName" label="道具名称" />
        <el-table-column prop="propType" label="道具类型" width="100" />
        <el-table-column prop="era" label="适配时代" width="100" />
        <el-table-column prop="bindTime" label="绑定时间" width="180" />
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button size="small" type="danger" @click="handleUnbind(scope.row)">解绑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="bindModalVisible" title="绑定道具到角色" width="600px">
      <el-form :model="bindForm" label-width="100px">
        <el-form-item label="选择剧本" required>
          <el-select v-model="bindForm.scriptThemeId" @change="onThemeChange">
            <el-option v-for="theme in themes" :key="theme.id" :label="`${theme.themeName}（${theme.era || '未设定时代'}）`" :value="theme.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择角色" required>
          <el-select v-model="bindForm.characterRoleId">
            <el-option v-for="role in filteredRoles" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="剧本时代">
          <el-tag v-if="bindTheme?.era" type="warning">{{ bindTheme.era }}</el-tag>
          <span v-else class="era-hint">该剧本未设定时代</span>
        </el-form-item>
        <el-form-item label="选择道具" required>
          <el-checkbox-group v-model="bindForm.propIds">
            <el-checkbox
              v-for="prop in props"
              :key="prop.id"
              :label="prop.id"
              :disabled="!isSameEra(prop.era)"
            >
              {{ prop.propCode }} - {{ prop.propName }} ({{ prop.era || '未设定' }} / {{ prop.propType }})
              <el-tag v-if="!isSameEra(prop.era)" type="danger" size="small">时代不符，请改选同代道具</el-tag>
            </el-checkbox>
          </el-checkbox-group>
          <div class="era-hint">
            仅可绑定与剧本「{{ bindTheme?.era || '未设定' }}」同时代的道具；跨时代道具已禁选。
            共 {{ sameEraProps.length }} 件同代道具可选。
          </div>
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="bindForm.operator" />
        </el-form-item>
        <el-form-item label="原因">
          <el-input type="textarea" v-model="bindForm.reason" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindModalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBind">确定绑定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="changeModalVisible" title="更换道具所属角色" width="600px">
      <el-form :model="changeForm" label-width="100px">
        <el-form-item label="选择剧本" required>
          <el-select v-model="changeForm.scriptThemeId" @change="onChangeTheme">
            <el-option v-for="theme in themes" :key="theme.id" :label="`${theme.themeName}（${theme.era || '未设定时代'}）`" :value="theme.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="原角色" required>
          <el-select v-model="changeForm.fromRoleId" @change="onChangeFromRole">
            <el-option v-for="role in changeRoles" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="新角色" required>
          <el-select v-model="changeForm.toRoleId">
            <el-option v-for="role in changeRoles" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="剧本时代">
          <el-tag v-if="changeTheme?.era" type="warning">{{ changeTheme.era }}</el-tag>
          <span v-else class="era-hint">该剧本未设定时代</span>
        </el-form-item>
        <el-form-item label="选择道具" required>
          <el-select v-model="changeForm.propId">
            <el-option
              v-for="prop in changeProps"
              :key="prop.id"
              :label="`${prop.propCode} - ${prop.propName}（${prop.era || '未设定'}）`"
              :value="prop.id"
            />
          </el-select>
          <div v-if="changeSelectedProp && !isChangePropSameEra(changeSelectedProp.era)" class="era-error">
            道具「{{ changeSelectedProp.propName }}」时代为「{{ changeSelectedProp.era || '未设定' }}」，
            与剧本时代「{{ changeTheme?.era || '未设定' }}」不符，不能更换到该剧本角色，请改选同代道具。
          </div>
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="changeForm.operator" />
        </el-form-item>
        <el-form-item label="原因">
          <el-input type="textarea" v-model="changeForm.reason" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="changeModalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleChange">确定更换</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { themeApi, roleApi, propApi, rolePropApi, type ScriptTheme, type CharacterRole, type Prop, type RolePropResponse } from '@/api'

const themes = ref<ScriptTheme[]>([])
const roles = ref<CharacterRole[]>([])
const props = ref<Prop[]>([])
const roleProps = ref<RolePropResponse[]>([])

const bindModalVisible = ref(false)
const changeModalVisible = ref(false)

const bindForm = ref({
  scriptThemeId: 0,
  characterRoleId: 0,
  propIds: [] as number[],
  operator: '',
  reason: ''
})

const changeForm = ref({
  scriptThemeId: 0,
  fromRoleId: 0,
  toRoleId: 0,
  propId: 0,
  operator: '',
  reason: ''
})

const filteredRoles = computed(() => {
  return roles.value.filter(r => r.scriptThemeId === bindForm.value.scriptThemeId)
})

const changeRoles = computed(() => {
  return roles.value.filter(r => r.scriptThemeId === changeForm.value.scriptThemeId)
})

const changeProps = computed(() => {
  if (!changeForm.value.fromRoleId) return []
  const rolePropsForRole = roleProps.value.filter(rp => rp.characterRoleId === changeForm.value.fromRoleId)
  const propIds = rolePropsForRole.map(rp => rp.propId)
  return props.value.filter(p => propIds.includes(p.id))
})

// 绑定弹窗当前所选剧本及其时代（核对基准）
const bindTheme = computed(() =>
  themes.value.find(t => t.id === bindForm.value.scriptThemeId) || null
)

// 更换弹窗当前所选剧本及其时代（核对基准）
const changeTheme = computed(() =>
  themes.value.find(t => t.id === changeForm.value.scriptThemeId) || null
)

const changeSelectedProp = computed(() =>
  props.value.find(p => p.id === changeForm.value.propId) || null
)

// 同代道具（与当前所选剧本时代一致）
const sameEraProps = computed(() =>
  props.value.filter(p => isSameEra(p.era))
)

// 时代核对：去空白后必须完全一致；任一方缺时代也视为不符
const eraEquals = (themeEra?: string, propEra?: string): boolean => {
  const t = (themeEra || '').trim()
  const p = (propEra || '').trim()
  return t !== '' && p !== '' && t === p
}

const isSameEra = (propEra?: string): boolean =>
  eraEquals(bindTheme.value?.era, propEra)

const isChangePropSameEra = (propEra?: string): boolean =>
  eraEquals(changeTheme.value?.era, propEra)

const onThemeChange = () => {
  bindForm.value.characterRoleId = 0
  bindForm.value.propIds = []
}

const onChangeTheme = () => {
  changeForm.value.fromRoleId = 0
  changeForm.value.toRoleId = 0
  changeForm.value.propId = 0
}

const onChangeFromRole = () => {
  changeForm.value.propId = 0
}

// 从 axios 异常中取出后端返回的提示语
const resolveError = (err: unknown, fallback: string): string => {
  const e = err as { response?: { data?: { message?: string } }; message?: string }
  return e?.response?.data?.message || e?.message || fallback
}

const loadData = async () => {
  const [themeRes, roleRes, propRes, rolePropRes] = await Promise.all([
    themeApi.getAll(),
    roleApi.getAll(),
    propApi.getAll(),
    rolePropApi.getAll()
  ])
  themes.value = themeRes
  roles.value = roleRes
  props.value = propRes
  roleProps.value = rolePropRes
}

const openBindModal = () => {
  bindForm.value = {
    scriptThemeId: themes.value[0]?.id || 0,
    characterRoleId: 0,
    propIds: [],
    operator: '',
    reason: ''
  }
  bindModalVisible.value = true
}

const openChangeModal = () => {
  changeForm.value = {
    scriptThemeId: themes.value[0]?.id || 0,
    fromRoleId: 0,
    toRoleId: 0,
    propId: 0,
    operator: '',
    reason: ''
  }
  changeModalVisible.value = true
}

const handleBind = async () => {
  if (!bindForm.value.scriptThemeId || !bindForm.value.characterRoleId || bindForm.value.propIds.length === 0) {
    ElMessage.error('请选择剧本、角色和道具')
    return
  }
  const theme = bindTheme.value
  if (!theme?.era?.trim()) {
    ElMessage.error('所选剧本未设定时代，无法核对道具时代')
    return
  }
  // 提交前再核对一遍：跨时代道具必须拦住，提示改选同代
  const mismatched = props.value.filter(
    p => bindForm.value.propIds.includes(p.id) && !eraEquals(theme.era, p.era)
  )
  if (mismatched.length > 0) {
    ElMessage({
      type: 'error',
      duration: 5000,
      message: `跨时代绑定被拦截：剧本「${theme.themeName}」时代为「${theme.era}」，${mismatched
        .map(p => `「${p.propName}」为${p.era ? '「' + p.era + '」' : '未设定时代'}`)
        .join('、')}，请改选同代道具`
    })
    return
  }
  try {
    await rolePropApi.bind(bindForm.value)
    ElMessage.success('绑定成功')
    bindModalVisible.value = false
    loadData()
  } catch (err) {
    ElMessage({ type: 'error', duration: 5000, message: resolveError(err, '绑定失败') })
  }
}

const handleUnbind = async (row: RolePropResponse) => {
  try {
    await ElMessageBox.confirm(`确定解绑道具「${row.propName}」与角色「${row.roleName}」？`, '提示', { type: 'warning' })
    await rolePropApi.unbind({ roleId: row.characterRoleId, propIds: [row.propId] })
    ElMessage.success('解绑成功')
    loadData()
  } catch {
    // cancelled
  }
}

const handleChange = async () => {
  if (!changeForm.value.scriptThemeId || !changeForm.value.fromRoleId || !changeForm.value.toRoleId || !changeForm.value.propId) {
    ElMessage.error('请完整填写表单')
    return
  }
  if (changeForm.value.fromRoleId === changeForm.value.toRoleId) {
    ElMessage.error('原角色和新角色不能相同')
    return
  }
  const theme = changeTheme.value
  const prop = changeSelectedProp.value
  if (!theme?.era?.trim()) {
    ElMessage.error('所选剧本未设定时代，无法核对道具时代')
    return
  }
  // 更换所属人物前核对道具时代与新角色所属剧本时代，不符不能换过去
  if (prop && !eraEquals(theme.era, prop.era)) {
    ElMessage({
      type: 'error',
      duration: 5000,
      message: `跨时代更换被拦截：道具「${prop.propName}」时代为「${prop.era || '未设定'}」，剧本「${theme.themeName}」时代为「${theme.era}」，请改选同代道具`
    })
    return
  }
  try {
    await rolePropApi.change(changeForm.value)
    ElMessage.success('更换成功')
    changeModalVisible.value = false
    loadData()
  } catch (err) {
    ElMessage({ type: 'error', duration: 5000, message: resolveError(err, '更换失败') })
  }
}

onMounted(loadData)
</script>

<style scoped>
.card-header {
  margin-bottom: 20px;
}

.card-header .el-button {
  margin-right: 10px;
}

.era-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}

.era-error {
  margin-top: 6px;
  font-size: 12px;
  color: #f56c6c;
  line-height: 1.6;
}
</style>