<template>
  <div class="history-record">
    <el-card>
      <div class="card-header">
        <el-select v-model="filterType" placeholder="筛选类型" style="width: 150px;" clearable>
          <el-option label="全部" value="" />
          <el-option label="绑定" value="绑定" />
          <el-option label="解绑" value="解绑" />
          <el-option label="更换" value="更换" />
        </el-select>
        <el-button @click="loadRecords" style="margin-left: 10px;">刷新</el-button>
      </div>
      <el-table :data="filteredRecords" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="changeType" label="变更类型" width="100">
          <template #default="scope">
            <el-tag :type="getChangeTypeTagType(scope.row.changeType)">
              {{ scope.row.changeType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="propName" label="道具名称" />
        <el-table-column prop="roleName" label="角色名称" width="120" />
        <el-table-column prop="themeName" label="剧本主题" width="120" />
        <el-table-column prop="beforeValue" label="变更前" show-overflow-tooltip />
        <el-table-column prop="afterValue" label="变更后" show-overflow-tooltip />
        <el-table-column prop="changeReason" label="变更原因" show-overflow-tooltip />
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="createdAt" label="变更时间" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { changeRecordApi, propApi, roleApi, themeApi, type PropChangeRecord, type Prop, type CharacterRole, type ScriptTheme } from '@/api'

const records = ref<(PropChangeRecord & { propName: string; roleName: string; themeName: string })[]>([])
const filterType = ref('')

const propsMap = ref<Record<number, Prop>>({})
const rolesMap = ref<Record<number, CharacterRole>>({})
const themesMap = ref<Record<number, ScriptTheme>>({})

const getChangeTypeTagType = (type: string) => {
  if (type === '绑定') return 'success'
  if (type === '解绑') return 'danger'
  if (type === '更换') return 'warning'
  return 'info'
}

const filteredRecords = computed(() => {
  if (!filterType.value) return records.value
  return records.value.filter(r => r.changeType === filterType.value)
})

const loadRecords = async () => {
  const [recordRes, propRes, roleRes, themeRes] = await Promise.all([
    changeRecordApi.getAll(),
    propApi.getAll(),
    roleApi.getAll(),
    themeApi.getAll()
  ])
  
  const propList = propRes
  const roleList = roleRes
  const themeList = themeRes
  
  propsMap.value = propList.reduce((acc, p) => ({ ...acc, [p.id]: p }), {})
  rolesMap.value = roleList.reduce((acc, r) => ({ ...acc, [r.id]: r }), {})
  themesMap.value = themeList.reduce((acc, t) => ({ ...acc, [t.id]: t }), {})
  
  records.value = recordRes.map(record => ({
    ...record,
    propName: propsMap.value[record.propId]?.propName || '',
    roleName: rolesMap.value[record.characterRoleId]?.roleName || '',
    themeName: themesMap.value[record.scriptThemeId]?.themeName || ''
  }))
}

onMounted(loadRecords)
</script>

<style scoped>
.card-header {
  margin-bottom: 20px;
}
</style>