<template>
  <div class="page-container">
    <div class="page-header">
      <h2>仓库管理</h2>
      <el-button type="primary" @click="open(null)" v-if="has('warehouse:write')">新增仓库</el-button>
    </div>

    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item><el-input v-model="query.keyword" placeholder="搜索名称或位置" clearable @keyup.enter="fetch"/></el-form-item>
        <el-form-item><el-select v-model="query.status" clearable placeholder="状态" style="width:110px"><el-option label="正常" :value="1"/><el-option label="停用" :value="0"/></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="fetch">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
      </el-form>
    </div>

    <div class="table-card" style="margin-top:16px">
      <el-table :data="data" v-loading="loading" stripe>
        <el-table-column prop="warehouseCode" label="编号" width="150"/>
        <el-table-column prop="name" label="名称" width="200"/>
        <el-table-column prop="location" label="位置" min-width="250" show-overflow-tooltip/>
        <el-table-column prop="status" label="状态" width="80"><template #default="{row}"><el-tag :type="row.status===1?'success':'info'" size="small">{{row.status===1?'正常':'停用'}}</el-tag></template></el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="165"/>
        <el-table-column prop="updatedAt" label="更新时间" width="165"/>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{row}"><el-button type="primary" link @click="open(row)" v-if="has('warehouse:write')">编辑</el-button><el-button link :type="row.status===1?'warning':'success'" @click="toggle(row)">{{row.status===1?'停用':'启用'}}</el-button></template>
        </el-table-column>
      </el-table>
      <div class="table-footer"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" @change="fetch"/></div>
    </div>

    <el-dialog v-model="dlg" :title="editing?'编辑仓库':'新增仓库'" width="480px" @close="formRef?.resetFields()">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" placeholder="如 主仓库"/></el-form-item>
        <el-form-item label="位置"><el-input v-model="form.location" type="textarea" :rows="2" placeholder="如 1号厂房一层"/></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" placeholder="存储条件、管理人员等"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="dlg=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { listWarehouses, createWarehouse, updateWarehouse, toggleWarehouseStatus } from '@/api/warehouse'
const userStore = useUserStore(); const has = p => userStore.hasPermission(p)
const loading=ref(false),saving=ref(false),dlg=ref(false),editing=ref(false),editId=ref(null),data=ref([]),total=ref(0),formRef=ref(null)
const query=reactive({page:1,size:10,keyword:'',status:null})
const form=reactive({name:'',location:'',remark:''})
const rules={name:[{required:true,message:'请输入仓库名称'}]}
async function fetch(){loading.value=true;try{const r=await listWarehouses(query);data.value=r.data.records;total.value=r.data.total}finally{loading.value=false}}
function reset(){query.keyword='';query.status=null;query.page=1;fetch()}
function open(row){editing.value=!!row;if(row){editId.value=row.id;Object.assign(form,row)}else{editId.value=null;Object.keys(form).forEach(k=>form[k]='')}dlg.value=true}
async function save(){if(!await formRef.value.validate().catch(()=>false))return;saving.value=true;try{editing.value?await updateWarehouse(editId.value,form):await createWarehouse(form);ElMessage.success(editing.value?'仓库已更新':'仓库已创建');dlg.value=false;fetch()}finally{saving.value=false}}
async function toggle(row){const a=row.status===1?'停用':'启用';await ElMessageBox.confirm(`确定${a}「${row.name}」？`,'操作确认',{type:'warning'});await toggleWarehouseStatus(row.id);ElMessage.success(`已${a}`);fetch()}
onMounted(fetch)
</script>
<style scoped>.page-header{display:flex;align-items:center;justify-content:space-between;margin-bottom:var(--spacing-lg)}.page-header h2{margin:0}.table-footer{display:flex;justify-content:flex-end;margin-top:var(--spacing-md)}</style>