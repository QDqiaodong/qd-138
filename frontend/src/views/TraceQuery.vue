<template>
  <div class="trace-query">
    <el-card>
      <div class="search-section">
        <el-radio-group v-model="searchType" @change="onSearchTypeChange">
          <el-radio-button label="role">按角色查询</el-radio-button>
          <el-radio-button label="theme">按剧本查询</el-radio-button>
          <el-radio-button label="prop">按道具查询</el-radio-button>
        </el-radio-group>
        
        <div v-if="searchType === 'role'" class="search-input">
          <el-input v-model="searchKeyword" placeholder="输入角色名称" style="width: 300px;" clearable>
            <template #append>
              <el-button @click="searchByRole">搜索</el-button>
            </template>
          </el-input>
        </div>
        
        <div v-if="searchType === 'theme'" class="search-input">
          <el-select v-model="searchThemeId" placeholder="选择剧本主题" style="width: 300px;" clearable>
            <el-option v-for="theme in themes" :key="theme.id" :label="theme.themeName" :value="theme.id" />
          </el-select>
          <el-button @click="searchByTheme" style="margin-left: 10px;">查询</el-button>
        </div>
        
        <div v-if="searchType === 'prop'" class="search-input">
          <el-select v-model="searchPropId" placeholder="选择道具" style="width: 300px;" clearable>
            <el-option v-for="prop in props" :key="prop.id" :label="`${prop.propCode} - ${prop.propName}`" :value="prop.id" />
          </el-select>
          <el-button @click="searchByProp" style="margin-left: 10px;">查询</el-button>
        </div>
      </div>

      <div v-if="searchType === 'role' && roleResult" class="result-section">
        <h3>角色：{{ roleResult.roleName }}（{{ roleResult.themeName }}）</h3>
        <el-table :data="roleResult.props" border>
          <el-table-column prop="propCode" label="道具编号" width="120" />
          <el-table-column prop="propName" label="道具名称" />
          <el-table-column prop="propType" label="道具类型" width="100" />
          <el-table-column prop="era" label="适配时代" width="100" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.status === '正常' ? 'success' : 'danger'">{{ scope.row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" show-overflow-tooltip />
        </el-table>
      </div>

      <div v-if="searchType === 'theme' && themeResult" class="result-section">
        <h3>剧本主题：{{ themeResult.themeName }}</h3>
        <p><strong>适配时代：</strong>{{ themeResult.era }} | <strong>难度：</strong>{{ themeResult.difficulty }}</p>
        <div v-for="role in themeResult.roles" :key="role.id" class="role-group">
          <h4>{{ role.roleName }}（{{ role.gender }}，{{ role.age }}岁）</h4>
          <el-table :data="role.props" border>
            <el-table-column prop="propCode" label="道具编号" width="120" />
            <el-table-column prop="propName" label="道具名称" />
            <el-table-column prop="propType" label="道具类型" width="100" />
            <el-table-column prop="era" label="适配时代" width="100" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="scope">
                <el-tag :type="scope.row.status === '正常' ? 'success' : 'danger'">{{ scope.row.status }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <div v-if="searchType === 'prop' && propResult.length > 0" class="result-section">
        <h3>道具溯源结果</h3>
        <el-table :data="propResult" border>
          <el-table-column prop="propCode" label="道具编号" width="120" />
          <el-table-column prop="propName" label="道具名称" />
          <el-table-column prop="propType" label="道具类型" width="100" />
          <el-table-column prop="era" label="适配时代" width="100" />
          <el-table-column prop="themeName" label="剧本主题" width="120" />
          <el-table-column prop="roleName" label="绑定角色" width="120" />
          <el-table-column prop="bindTime" label="绑定时间" width="180" />
        </el-table>
      </div>

      <div v-if="!roleResult && !themeResult && !propResult && searched" class="empty-result">
        <el-empty description="暂无查询结果" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { themeApi, propApi, rolePropApi, type ScriptTheme, type Prop, type RolePropsResponse, type ScriptThemeDetailResponse, type RolePropResponse } from '@/api'

const searchType = ref<'role' | 'theme' | 'prop'>('role')
const searchKeyword = ref('')
const searchThemeId = ref<number | null>(null)
const searchPropId = ref<number | null>(null)
const searched = ref(false)

const themes = ref<ScriptTheme[]>([])
const props = ref<Prop[]>([])
const roleResult = ref<RolePropsResponse | null>(null)
const themeResult = ref<ScriptThemeDetailResponse | null>(null)
const propResult = ref<RolePropResponse[]>([])

const onSearchTypeChange = () => {
  roleResult.value = null
  themeResult.value = null
  propResult.value = []
  searched.value = false
}

const loadOptions = async () => {
  const [themeRes, propRes] = await Promise.all([
    themeApi.getAll(),
    propApi.getAll()
  ])
  themes.value = themeRes
  props.value = propRes
}

const searchByRole = async () => {
  if (!searchKeyword.value.trim()) {
    return
  }
  searched.value = true
  const res = await rolePropApi.searchByRoleName(searchKeyword.value)
  roleResult.value = res || null
  themeResult.value = null
  propResult.value = []
}

const searchByTheme = async () => {
  if (!searchThemeId.value) {
    return
  }
  searched.value = true
  const res = await themeApi.getDetail(searchThemeId.value)
  themeResult.value = res || null
  roleResult.value = null
  propResult.value = []
}

const searchByProp = async () => {
  if (!searchPropId.value) {
    return
  }
  searched.value = true
  const res = await rolePropApi.getByPropId(searchPropId.value)
  propResult.value = res || []
  roleResult.value = null
  themeResult.value = null
}

onMounted(loadOptions)
</script>

<style scoped>
.search-section {
  margin-bottom: 20px;
}

.search-input {
  margin-top: 15px;
}

.result-section h3 {
  margin-bottom: 15px;
  color: #333;
}

.result-section p {
  margin-bottom: 20px;
  color: #666;
}

.role-group {
  margin-bottom: 25px;
  padding: 15px;
  background: #fafafa;
  border-radius: 8px;
}

.role-group h4 {
  margin-bottom: 10px;
  color: #444;
}

.empty-result {
  margin-top: 50px;
}
</style>