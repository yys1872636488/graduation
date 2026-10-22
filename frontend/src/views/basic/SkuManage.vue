<template>
  <div class="page-container">
    <div class="page-header">
      <h2>SKU管理</h2>
      <el-button type="primary" @click="open(null)" v-if="has('sku:write')">新增SKU</el-button>
    </div>

    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item><el-select v-model="query.productId" clearable filterable placeholder="所属产品" style="width:200px"><el-option v-for="p in productList" :key="p.id" :label="`[${p.productCode}] ${p.name}`" :value="p.id"/></el-select></el-form-item>
        <el-form-item><el-select v-model="query.color" clearable placeholder="颜色" style="width:110px"><el-option v-for="c in COLOR_OPTIONS" :key="c" :label="c" :value="c"/></el-select></el-form-item>
        <el-form-item><el-select v-model="query.sizeLabel" clearable placeholder="尺码" style="width:100px"><el-option v-for="s in SIZE_OPTIONS" :key="s" :label="s" :value="s"/></el-select></el-form-item>
        <el-form-item><el-select v-model="query.status" clearable placeholder="状态" style="width:100px"><el-option label="正常" :value="1"/><el-option label="停用" :value="0"/></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="fetch">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
      </el-form>
    </div>

    <div class="table-card" style="margin-top:16px">
      <el-table :data="data" v-loading="loading" stripe>
        <el-table-column prop="skuCode" label="SKU编码" width="160"/>
        <el-table-column label="所属产品" min-width="180" show-overflow-tooltip><template #default="{row}">{{ getProduct(row.productId) }}</template></el-table-column>
        <el-table-column prop="color" label="颜色" width="90"/>
        <el-table-column prop="sizeLabel" label="尺码" width="80"/>
        <el-table-column prop="unit" label="单位" width="70"/>
        <el-table-column prop="status" label="状态" width="80"><template #default="{row}"><el-tag :type="row.status===1?'success':'info'" size="small">{{row.status===1?'正常':'停用'}}</el-tag></template></el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="165"/>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{row}"><el-button type="primary" link @click="open(row)" v-if="has('sku:write')">编辑</el-button><el-button link :type="row.status===1?'warning':'success'" @click="toggle(row)">{{row.status===1?'停用':'启用'}}</el-button></template>
        </el-table-column>
      </el-table>
      <div class="table-footer"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" @change="fetch"/></div>
    </div>

    <el-dialog v-model="dlg" :title="editing?'编辑SKU':'新增SKU'" width="480px" @close="formRef?.resetFields()">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="所属产品" prop="productId"><el-select v-model="form.productId" filterable placeholder="选择产品" style="width:100%"><el-option v-for="p in productList" :key="p.id" :label="`[${p.productCode}] ${p.name}`" :value="p.id"/></el-select></el-form-item>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="颜色" prop="color"><el-select v-model="form.color" placeholder="选择" style="width:100%" filterable><el-option v-for="c in COLOR_OPTIONS" :key="c" :label="c" :value="c"/></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="尺码" prop="sizeLabel"><el-select v-model="form.sizeLabel" placeholder="选择" style="width:100%"><el-option v-for="s in SIZE_OPTIONS" :key="s" :label="s" :value="s"/></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="单位" prop="unit"><el-select v-model="form.unit" placeholder="选择" style="width:100%"><el-option v-for="u in SKU_UNITS" :key="u" :label="u" :value="u"/></el-select></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer><el-button @click="dlg=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { listSkus, createSku, updateSku, toggleSkuStatus } from '@/api/sku'
import { listProducts } from '@/api/product'
import { COLOR_OPTIONS, SIZE_OPTIONS, SKU_UNITS } from '@/utils/constants'
const userStore = useUserStore(); const has = p => userStore.hasPermission(p)
const loading=ref(false),saving=ref(false),dlg=ref(false),editing=ref(false),editId=ref(null),data=ref([]),total=ref(0),formRef=ref(null),productList=ref([])
const query=reactive({page:1,size:10,productId:null,color:'',sizeLabel:'',status:null})
const form=reactive({productId:null,color:'',sizeLabel:'',unit:'件'})
const rules={productId:[{required:true}],color:[{required:true}],sizeLabel:[{required:true}],unit:[{required:true}]}
function getProduct(pid){const p=productList.value.find(p=>p.id===pid);return p?`[${p.productCode}] ${p.name}`:`ID:${pid}`}
async function fetchProducts(){try{const r=await listProducts({page:1,size:9999,status:1});productList.value=r.data.records||[]}catch{}}
async function fetch(){loading.value=true;try{const r=await listSkus(query);data.value=r.data.records;total.value=r.data.total}finally{loading.value=false}}
function reset(){query.productId=null;query.color='';query.sizeLabel='';query.status=null;query.page=1;fetch()}
function open(row){editing.value=!!row;if(row){editId.value=row.id;Object.assign(form,row)}else{editId.value=null;Object.keys(form).forEach(k=>form[k]=k==='unit'?'件':k==='productId'?null:'')}dlg.value=true}
async function save(){if(!await formRef.value.validate().catch(()=>false))return;saving.value=true;try{editing.value?await updateSku(editId.value,form):await createSku(form);ElMessage.success(editing.value?'SKU已更新':'SKU已创建');dlg.value=false;fetch()}finally{saving.value=false}}
async function toggle(row){const a=row.status===1?'停用':'启用';await ElMessageBox.confirm(`确定${a}「${row.skuCode}」？`,'操作确认',{type:'warning'});await toggleSkuStatus(row.id);ElMessage.success(`已${a}`);fetch()}
onMounted(()=>{fetchProducts();fetch()})
</script>
<style scoped>.page-header{display:flex;align-items:center;justify-content:space-between;margin-bottom:var(--spacing-lg)}.page-header h2{margin:0}.table-footer{display:flex;justify-content:flex-end;margin-top:var(--spacing-md)}</style>