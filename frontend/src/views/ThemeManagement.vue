<template>
  <div class="theme-management">
    <el-card>
      <div class="card-header">
        <el-button type="primary" @click="openAddModal">新增剧本主题</el-button>
      </div>
      <el-table :data="themes" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="themeName" label="主题名称" />
        <el-table-column prop="era" label="适配时代" width="100" />
        <el-table-column prop="difficulty" label="难度" width="100">
          <template #default="scope">
            <el-tag :type="getDifficultyTagType(scope.row.difficulty)">{{ scope.row.difficulty }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button size="small" @click="viewDetail(scope.row)">详情</el-button>
            <el-button size="small" type="primary" @click="openEditModal(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="modalVisible" :title="isEdit ? '编辑剧本主题' : '新增剧本主题'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="主题名称" required>
          <el-input v-model="form.themeName" />
        </el-form-item>
        <el-form-item label="适配时代">
          <el-select v-model="form.era">
            <el-option label="民国" value="民国" />
            <el-option label="古代" value="古代" />
            <el-option label="古风" value="古风" />
            <el-option label="仙侠" value="仙侠" />
            <el-option label="未来" value="未来" />
            <el-option label="现代" value="现代" />
            <el-option label="悬疑" value="悬疑" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="form.difficulty">
            <el-option label="简单" value="简单" />
            <el-option label="中等" value="中等" />
            <el-option label="困难" value="困难" />
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

    <el-dialog v-model="detailVisible" title="剧本主题详情" width="700px">
      <div v-if="detail" class="detail-content">
        <h3>{{ detail.themeName }}</h3>
        <p><strong>适配时代：</strong>{{ detail.era }}</p>
        <p><strong>难度：</strong>{{ detail.difficulty }}</p>
        <p><strong>描述：</strong>{{ detail.description }}</p>
        <div class="roles-section">
          <h4>角色列表</h4>
          <el-table :data="detail.roles" border>
            <el-table-column prop="roleName" label="角色名称" />
            <el-table-column prop="gender" label="性别" width="80" />
            <el-table-column prop="age" label="年龄" width="80" />
            <el-table-column prop="description" label="描述" show-overflow-tooltip />
            <el-table-column label="配套道具" width="200">
              <template #default="scope">
                <el-tag v-for="prop in scope.row.props" :key="prop.id" size="small" style="margin: 2px;">
                  {{ prop.propName }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { themeApi, type ScriptTheme, type ScriptThemeDetailResponse } from '@/api'

const themes = ref<ScriptTheme[]>([])
const modalVisible = ref(false)
const detailVisible = ref(false)
const isEdit = ref(false)
const form = ref<Omit<ScriptTheme, 'id' | 'createdAt' | 'updatedAt'>>({
  themeName: '',
  description: '',
  era: '',
  difficulty: '中等'
})
const detail = ref<ScriptThemeDetailResponse | null>(null)
const editId = ref<number | null>(null)

const getDifficultyTagType = (difficulty: string) => {
  if (difficulty === '简单') return 'success'
  if (difficulty === '困难') return 'danger'
  return 'warning'
}

const loadThemes = async () => {
  const res = await themeApi.getAll()
  themes.value = res
}

const openAddModal = () => {
  isEdit.value = false
  editId.value = null
  form.value = { themeName: '', description: '', era: '', difficulty: '中等' }
  modalVisible.value = true
}

const openEditModal = (row: ScriptTheme) => {
  isEdit.value = true
  editId.value = row.id
  form.value = {
    themeName: row.themeName,
    description: row.description,
    era: row.era,
    difficulty: row.difficulty
  }
  modalVisible.value = true
}

const viewDetail = async (row: ScriptTheme) => {
  const res = await themeApi.getDetail(row.id)
  detail.value = res
  detailVisible.value = true
}

const handleSubmit = async () => {
  if (!form.value.themeName) {
    ElMessage.error('请输入主题名称')
    return
  }
  try {
    if (isEdit.value && editId.value) {
      await themeApi.update(editId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await themeApi.create(form.value)
      ElMessage.success('创建成功')
    }
    modalVisible.value = false
    loadThemes()
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleDelete = async (row: ScriptTheme) => {
  try {
    await ElMessageBox.confirm(`确定删除剧本主题「${row.themeName}」？`, '提示', { type: 'warning' })
    await themeApi.delete(row.id)
    ElMessage.success('删除成功')
    loadThemes()
  } catch {
    // cancelled
  }
}

onMounted(loadThemes)
</script>

<style scoped>
.card-header {
  margin-bottom: 20px;
}

.detail-content h3 {
  margin-bottom: 15px;
  color: #333;
}

.detail-content p {
  margin-bottom: 10px;
}

.roles-section {
  margin-top: 20px;
}

.roles-section h4 {
  margin-bottom: 15px;
  color: #666;
}
</style>