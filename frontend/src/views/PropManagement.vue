<template>
  <div class="prop-management">
    <el-card>
      <div class="card-header">
        <el-button type="primary" @click="openAddModal">新增道具</el-button>
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
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { propApi, type Prop } from '@/api'

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
  } catch {
    ElMessage.error('操作失败')
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
</script>

<style scoped>
.card-header {
  margin-bottom: 20px;
}
</style>