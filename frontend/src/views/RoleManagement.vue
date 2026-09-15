<template>
  <div class="role-management">
    <el-card>
      <div class="card-header">
        <el-button type="primary" @click="openAddModal">新增角色</el-button>
        <el-select v-model="filterThemeId" placeholder="按剧本筛选" style="margin-left: 20px; width: 200px;" clearable>
          <el-option v-for="theme in themes" :key="theme.id" :label="theme.themeName" :value="theme.id" />
        </el-select>
      </div>
      <el-table :data="filteredRoles" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="roleName" label="角色名称" />
        <el-table-column prop="themeName" label="所属剧本" width="120" />
        <el-table-column prop="gender" label="性别" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.gender === '男' ? 'info' : 'danger'">{{ scope.row.gender }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="80" />
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

    <el-dialog v-model="modalVisible" :title="isEdit ? '编辑角色' : '新增角色'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="角色名称" required>
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="所属剧本" required>
          <el-select v-model="form.scriptThemeId">
            <el-option v-for="theme in themes" :key="theme.id" :label="theme.themeName" :value="theme.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="form.gender">
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
          </el-select>
        </el-form-item>
        <el-form-item label="年龄">
          <el-input-number v-model="form.age" :min="0" :max="100" />
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
import { ref, computed, onMounted, h } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { themeApi, roleApi, getErrorMessage, type ScriptTheme, type CharacterRole } from '@/api'

const themes = ref<ScriptTheme[]>([])
const roles = ref<CharacterRole[]>([])
const modalVisible = ref(false)
const isEdit = ref(false)
const filterThemeId = ref<number | null>(null)
const form = ref<Omit<CharacterRole, 'id' | 'createdAt' | 'updatedAt' | 'themeName'>>({
  roleName: '',
  scriptThemeId: 0,
  gender: '',
  age: 0,
  description: ''
})
const editId = ref<number | null>(null)

const filteredRoles = computed(() => {
  if (!filterThemeId.value) return roles.value
  return roles.value.filter(r => r.scriptThemeId === filterThemeId.value)
})

const loadThemes = async () => {
  const res = await themeApi.getAll()
  themes.value = res
}

// 所属剧本以后端按当前主题实时关联返回的 themeName 为准，
// 本地主题列表仅作兜底，确保改名刷新后显示新名
const resolveThemeName = (role: CharacterRole): string =>
  role.themeName || themes.value.find(t => t.id === role.scriptThemeId)?.themeName || ''

const loadRoles = async () => {
  const res = await roleApi.getAll()
  roles.value = res.map(role => ({ ...role, themeName: resolveThemeName(role) }))
}

const openAddModal = () => {
  isEdit.value = false
  editId.value = null
  form.value = { roleName: '', scriptThemeId: themes.value[0]?.id || 0, gender: '', age: 0, description: '' }
  modalVisible.value = true
}

const openEditModal = (row: CharacterRole) => {
  isEdit.value = true
  editId.value = row.id
  form.value = {
    roleName: row.roleName,
    scriptThemeId: row.scriptThemeId,
    gender: row.gender,
    age: row.age,
    description: row.description
  }
  modalVisible.value = true
}

const handleSubmit = async () => {
  if (!form.value.roleName) {
    ElMessage.error('请输入角色名称')
    return
  }
  try {
    if (isEdit.value && editId.value) {
      await roleApi.update(editId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await roleApi.create(form.value)
      ElMessage.success('创建成功')
    }
    modalVisible.value = false
    loadRoles()
  } catch {
    ElMessage.error('操作失败')
  }
}

// 默认删除走 DELETE：人物还排在任意一场时后端一定拦住，提示里写出卡在哪几场，
// 不会顺带清排班、也不会删人物。只有场务在拦截框里显式选择「撤下并删除」，
// 才走另一条路：先撤各场排班，再连人带档拿掉，撤完场次会从已排好打回排班中。
const handleDelete = async (row: CharacterRole) => {
  try {
    await ElMessageBox.confirm(`确定删除角色「${row.roleName}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await roleApi.delete(row.id)
    ElMessage.success('删除成功')
    loadRoles()
  } catch (err) {
    // 还在场次里：把后端列出的卡点场次摆给场务，另给一条明确分开的「撤下并删除」路
    const blocked = getErrorMessage(err, '删除失败，请刷新后重试')
    const blockedMessage = h('div', { class: 'role-delete-blocked' }, [
      h('p', { class: 'blocked-reason' }, blocked),
      h('p', { class: 'blocked-action' },
        `要连人带档一起拿掉吗？这会先撤掉以上各场中「${row.roleName}」的排班再删除，相关场次会从「已排好」打回「排班中」。`)
    ])
    try {
      await ElMessageBox.confirm(
        blockedMessage,
        '删除被拦住',
        {
          confirmButtonText: '撤下并删除',
          cancelButtonText: '我再想想',
          type: 'warning',
          distinguishCancelAndClose: true
        }
      )
    } catch {
      // 场务取消或关闭：人物必须原样保留，拦截已经在后端生效
      return
    }
    try {
      await roleApi.unassignAndDelete(row.id)
      ElMessage.success('已撤下各场排班并删除该人物')
      loadRoles()
    } catch (err2) {
      ElMessage.error(getErrorMessage(err2, '撤下并删除失败，请刷新后重试'))
    }
  }
}

onMounted(async () => {
  await loadThemes()
  await loadRoles()
})
</script>

<style scoped>
.card-header {
  margin-bottom: 20px;
}
</style>

<!-- 提示框由 ElMessageBox 传送节点外渲染，样式不能加 scoped -->
<style>
.role-delete-blocked .blocked-reason {
  margin: 0 0 12px;
  color: #e6a23c;
  line-height: 1.6;
}

.role-delete-blocked .blocked-action {
  margin: 0;
  color: #606266;
  line-height: 1.6;
}
</style>