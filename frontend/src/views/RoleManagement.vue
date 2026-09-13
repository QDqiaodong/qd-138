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
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { themeApi, roleApi, type ScriptTheme, type CharacterRole } from '@/api'

const themes = ref<ScriptTheme[]>([])
const roles = ref<(CharacterRole & { themeName: string })[]>([])
const modalVisible = ref(false)
const isEdit = ref(false)
const filterThemeId = ref<number | null>(null)
const form = ref<Omit<CharacterRole, 'id' | 'createdAt' | 'updatedAt'>>({
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

const loadRoles = async () => {
  const res = await roleApi.getAll()
  const roleList = res
  roles.value = roleList.map(role => ({
    ...role,
    themeName: themes.value.find(t => t.id === role.scriptThemeId)?.themeName || ''
  }))
}

const openAddModal = () => {
  isEdit.value = false
  editId.value = null
  form.value = { roleName: '', scriptThemeId: themes.value[0]?.id || 0, gender: '', age: 0, description: '' }
  modalVisible.value = true
}

const openEditModal = (row: CharacterRole & { themeName: string }) => {
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

const handleDelete = async (row: CharacterRole & { themeName: string }) => {
  try {
    await ElMessageBox.confirm(`确定删除角色「${row.roleName}」？`, '提示', { type: 'warning' })
    await roleApi.delete(row.id)
    ElMessage.success('删除成功')
    loadRoles()
  } catch {
    // cancelled
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