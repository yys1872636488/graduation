<template>
  <div class="page-container">
    <div class="page-header">
      <h2>产品管理</h2>
      <el-button type="primary" @click="open(null)" v-if="has('product:write')">新增产品</el-button>
    </div>

    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item><el-select v-model="query.category" clearable placeholder="品类" style="width:120px"><el-option v-for="c in PRODUCT_CATEGORIES" :key="c" :label="c" :value="c"/></el-select></el-form-item>
        <el-form-item><el-select v-model="query.season" clearable placeholder="季节" style="width:120px"><el-option v-for="s in SEASON_OPTIONS" :key="s" :label="s" :value="s"/></el-select></el-form-item>
        <el-form-item><el-input v-model="query.keyword" placeholder="搜索名称" clearable @keyup.enter="fetch"/></el-form-item>
        <el-form-item><el-select v-model="query.status" clearable placeholder="状态" style="width:110px"><el-option label="正常" :value="1"/><el-option label="停用" :value="0"/></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="fetch">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
      </el-form>
    </div>

    <div class="table-card" style="margin-top:16px">
      <el-table :data="data" v-loading="loading" stripe>
        <el-table-column prop="productCode" label="编号" width="150"/>
        <el-table-column prop="name" label="名称" width="180"/>
        <el-table-column prop="category" label="品类" width="90"/>
        <el-table-column prop="style" label="款式" width="100"/>
        <el-table-column prop="season" label="季节" width="100"/>
        <el-table-column prop="status" label="状态" width="80"><template #default="{row}"><el-tag :type="row.status===1?'success':'info'" size="small">{{row.status===1?'正常':'停用'}}</el-tag></template></el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="165"/>
        <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip/>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{row}"><el-button type="primary" link @click="open(row)" v-if="has('product:write')">编辑</el-button><el-button link :type="row.status===1?'warning':'success'" @click="toggle(row)">{{row.status===1?'停用':'启用'}}</el-button></template>
        </el-table-column>
      </el-table>
      <div class="table-footer"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" @change="fetch"/></div>
    </div>

    <el-dialog v-model="dlg" :title="editing?'编辑产品':'新增产品'" width="540px" @close="formRef?.resetFields()">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" placeholder="如 经典款圆领T恤"/></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="品类" prop="category"><el-select v-model="form.category" placeholder="选择品类" style="width:100%"><el-option v-for="c in PRODUCT_CATEGORIES" :key="c" :label="c" :value="c"/></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="款式" prop="style"><el-select v-model="form.style" placeholder="选择款式" style="width:100%" filterable clearable><el-option v-for="s in PRODUCT_STYLES" :key="s" :label="s" :value="s"/></el-select></el-form-item></el-col>
        </el-row>
        <el-form-item label="季节" prop="season"><el-select v-model="form.season" placeholder="选择季节" style="width:100%"><el-option v-for="s in SEASON_OPTIONS" :key="s" :label="s" :value="s"/></el-select></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" placeholder="产品描述、面料成分、适用场景"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="dlg=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { listProducts, createProduct, updateProduct, toggleProductStatus } from '@/api/product'
import { PRODUCT_CATEGORIES, PRODUCT_STYLES, SEASON_OPTIONS } from '@/utils/constants'
const userStore = useUserStore(); const has = p => userStore.hasPermission(p)
const loading=ref(false),saving=ref(false),dlg=ref(false),editing=ref(false),editId=ref(null),data=ref([]),total=ref(0),formRef=ref(null)
const query=reactive({page:1,size:10,keyword:'',category:'',season:'',status:null})
const form=reactive({name:'',category:'',style:'',season:'',description:''})
const rules={name:[{required:true}],category:[{required:true}],season:[{required:true}]}
async function fetch(){loading.value=true;try{const r=await listProducts(query);data.value=r.data.records;total.value=r.data.total}finally{loading.value=false}}
function reset(){query.keyword='';query.category='';query.season='';query.status=null;query.page=1;fetch()}
function open(row){editing.value=!!row;if(row){editId.value=row.id;Object.assign(form,row)}else{editId.value=null;Object.keys(form).forEach(k=>form[k]='')}dlg.value=true}
async function save(){if(!await formRef.value.validate().catch(()=>false))return;saving.value=true;try{editing.value?await updateProduct(editId.value,form):await createProduct(form);ElMessage.success(editing.value?'产品已更新':'产品已创建');dlg.value=false;fetch()}finally{saving.value=false}}
async function toggle(row){const a=row.status===1?'停用':'启用';await ElMessageBox.confirm(`确定${a}「${row.name}」？`,'操作确认',{type:'warning'});await toggleProductStatus(row.id);ElMessage.success(`已${a}`);fetch()}
onMounted(fetch)
</script>
<style scoped>.page-header{display:flex;align-items:center;justify-content:space-between;margin-bottom:var(--spacing-lg)}.page-header h2{margin:0}.table-footer{display:flex;justify-content:flex-end;margin-top:var(--spacing-md)}</style>