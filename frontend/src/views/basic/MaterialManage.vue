<template>
  <div class="page-container">
    <div class="page-header">
      <h2>原材料管理</h2>
      <el-button type="primary" @click="open(null)" v-if="has('material:write')">新增原材料</el-button>
    </div>

    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item><el-select v-model="query.category" clearable placeholder="类别" style="width:120px"><el-option v-for="c in MATERIAL_CATEGORIES" :key="c" :label="c" :value="c"/></el-select></el-form-item>
        <el-form-item><el-input v-model="query.keyword" placeholder="搜索名称" clearable @keyup.enter="fetch"/></el-form-item>
        <el-form-item><el-select v-model="query.status" clearable placeholder="状态" style="width:110px"><el-option label="正常" :value="1"/><el-option label="停用" :value="0"/></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="fetch">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
      </el-form>
    </div>

    <div class="table-card" style="margin-top:16px">
      <el-table :data="data" v-loading="loading" stripe>
        <el-table-column prop="materialCode" label="编号" width="150"/>
        <el-table-column prop="name" label="名称" width="160"/>
        <el-table-column prop="category" label="类别" width="90"/>
        <el-table-column prop="specification" label="规格" width="130"/>
        <el-table-column prop="color" label="颜色" width="80"/>
        <el-table-column prop="unit" label="单位" width="70"/>
        <el-table-column label="默认供应商" min-width="150" show-overflow-tooltip><template #default="{row}">{{ getSupplier(row.defaultSupplierId) }}</template></el-table-column>
        <el-table-column prop="status" label="状态" width="80"><template #default="{row}"><el-tag :type="row.status===1?'success':'info'" size="small">{{row.status===1?'正常':'停用'}}</el-tag></template></el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="165"/>
        <el-table-column prop="updatedAt" label="更新时间" width="165"/>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{row}"><el-button type="primary" link @click="open(row)" v-if="has('material:write')">编辑</el-button><el-button link :type="row.status===1?'warning':'success'" @click="toggle(row)">{{row.status===1?'停用':'启用'}}</el-button></template>
        </el-table-column>
      </el-table>
      <div class="table-footer"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" @change="fetch"/></div>
    </div>

    <el-dialog v-model="dlg" :title="editing?'编辑原材料':'新增原材料'" width="560px" @close="formRef?.resetFields()">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" placeholder="如 纯棉斜纹布"/></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="类别" prop="category"><el-select v-model="form.category" placeholder="选择类别" style="width:100%"><el-option v-for="c in MATERIAL_CATEGORIES" :key="c" :label="c" :value="c"/></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="颜色" prop="color"><el-select v-model="form.color" placeholder="选择颜色" style="width:100%" filterable clearable><el-option v-for="c in COLOR_OPTIONS" :key="c" :label="c" :value="c"/></el-select></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="规格"><el-input v-model="form.specification" placeholder="如 200g/m²"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="单位" prop="unit"><el-select v-model="form.unit" placeholder="选择单位" style="width:100%"><el-option v-for="u in MATERIAL_UNITS" :key="u" :label="u" :value="u"/></el-select></el-form-item></el-col>
        </el-row>
        <el-form-item label="默认供应商"><el-select v-model="form.defaultSupplierId" placeholder="可选" style="width:100%" clearable filterable><el-option v-for="s in supplierList" :key="s.id" :label="`[${s.supplierCode}] ${s.name}`" :value="s.id"/></el-select></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" placeholder="其他说明"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="dlg=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { listMaterials, createMaterial, updateMaterial, toggleMaterialStatus } from '@/api/material'
import { listSuppliers } from '@/api/supplier'
import { MATERIAL_CATEGORIES, COLOR_OPTIONS, MATERIAL_UNITS } from '@/utils/constants'
const userStore = useUserStore(); const has = p => userStore.hasPermission(p)
const loading=ref(false),saving=ref(false),dlg=ref(false),editing=ref(false),editId=ref(null),data=ref([]),total=ref(0),formRef=ref(null),supplierList=ref([])
const query=reactive({page:1,size:10,keyword:'',category:'',status:null})
const form=reactive({name:'',category:'',specification:'',color:'',unit:'',remark:'',defaultSupplierId:null})
const rules={name:[{required:true}],category:[{required:true}],unit:[{required:true}]}
function getSupplier(id){if(!id)return '—';const s=supplierList.value.find(s=>s.id===id);return s?`[${s.supplierCode}] ${s.name}`:'—'}
async function fetchSuppliers(){try{const r=await listSuppliers({page:1,size:9999,status:1});supplierList.value=r.data.records||[]}catch{}}
async function fetch(){loading.value=true;try{const r=await listMaterials(query);data.value=r.data.records;total.value=r.data.total}finally{loading.value=false}}
function reset(){query.keyword='';query.category='';query.status=null;query.page=1;fetch()}
function open(row){editing.value=!!row;if(row){editId.value=row.id;Object.assign(form,row)}else{editId.value=null;Object.keys(form).forEach(k=>form[k]=k==='defaultSupplierId'?null:'')}dlg.value=true}
async function save(){if(!await formRef.value.validate().catch(()=>false))return;saving.value=true;try{editing.value?await updateMaterial(editId.value,form):await createMaterial(form);ElMessage.success(editing.value?'原材料已更新':'原材料已创建');dlg.value=false;fetch()}finally{saving.value=false}}
async function toggle(row){const a=row.status===1?'停用':'启用';await ElMessageBox.confirm(`确定${a}「${row.name}」？`,'操作确认',{type:'warning'});await toggleMaterialStatus(row.id);ElMessage.success(`已${a}`);fetch()}
onMounted(()=>{fetchSuppliers();fetch()})
</script>
<style scoped>.page-header{display:flex;align-items:center;justify-content:space-between;margin-bottom:var(--spacing-lg)}.page-header h2{margin:0}.table-footer{display:flex;justify-content:flex-end;margin-top:var(--spacing-md)}</style>