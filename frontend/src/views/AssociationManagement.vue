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
            <el-option v-for="theme in themes" :key="theme.id" :label="theme.themeName" :value="theme.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择角色" required>
          <el-select v-model="bindForm.characterRoleId">
            <el-option v-for="role in filteredRoles" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择道具" required>
          <el-checkbox-group v-model="bindForm.propIds">
            <el-checkbox v-for="prop in props" :key="prop.id" :label="prop.id">
              {{ prop.propCode }} - {{ prop.propName }} ({{ prop.era }} / {{ prop.propType }})
            </el-checkbox>
          </el-checkbox-group>
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
            <el-option v-for="theme in themes" :key="theme.id" :label="theme.themeName" :value="theme.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="原角色" required>
          <el-select v-model="changeForm.fromRoleId">
            <el-option v-for="role in changeRoles" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="新角色" required>
          <el-select v-model="changeForm.toRoleId">
            <el-option v-for="role in changeRoles" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择道具" required>
          <el-select v-model="changeForm.propId">
            <el-option v-for="prop in changeProps" :key="prop.id" :label="prop.propName" :value="prop.id" />
          </el-select>
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

const onThemeChange = () => {
  bindForm.value.characterRoleId = 0
  bindForm.value.propIds = []
}

const onChangeTheme = () => {
  changeForm.value.fromRoleId = 0
  changeForm.value.toRoleId = 0
  changeForm.value.propId = 0
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
  if (!bindForm.value.characterRoleId || bindForm.value.propIds.length === 0) {
    ElMessage.error('请选择角色和道具')
    return
  }
  try {
    await rolePropApi.bind(bindForm.value)
    ElMessage.success('绑定成功')
    bindModalVisible.value = false
    loadData()
  } catch {
    ElMessage.error('绑定失败')
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
  if (!changeForm.value.fromRoleId || !changeForm.value.toRoleId || !changeForm.value.propId) {
    ElMessage.error('请完整填写表单')
    return
  }
  if (changeForm.value.fromRoleId === changeForm.value.toRoleId) {
    ElMessage.error('原角色和新角色不能相同')
    return
  }
  try {
    await rolePropApi.change(changeForm.value)
    ElMessage.success('更换成功')
    changeModalVisible.value = false
    loadData()
  } catch {
    ElMessage.error('更换失败')
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
</style>