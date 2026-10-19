// 菜单工具：把后端 admin_menu 数据转成前端路由与 el-menu 数据

/**
 * 将扁平菜单列表（含 parentId）递归组装成树
 * @param {Array} list 后端菜单列表
 * @returns {Array} 树形菜单（children 嵌套）
 */
export function buildMenuTree(list) {
  const map = {}
  const tree = []
  list.forEach((item) => {
    map[item.id] = { ...item, children: [] }
  })
  list.forEach((item) => {
    const node = map[item.id]
    if (item.parentId && item.parentId !== 0 && map[item.parentId]) {
      map[item.parentId].children.push(node)
    } else {
      tree.push(node)
    }
  })
  // 按 sort 升序
  const sortFn = (a, b) => (a.sort || 0) - (b.sort || 0)
  const sortTree = (nodes) => {
    nodes.sort(sortFn)
    nodes.forEach((n) => n.children && n.children.length && sortTree(n.children))
  }
  sortTree(tree)
  return tree
}

/**
 * 将树形菜单转换成 vue-router 子路由（仅 type=2 的页面生成路由）
 * component 形如 'views/user/UserList.vue'，动态 import
 * @param {Array} tree 树形菜单
 * @returns {Array} routes
 */
export function buildRoutes(tree) {
  const routes = []
  const walk = (nodes) => {
    nodes.forEach((node) => {
      if (node.type === 2 && node.path && node.component) {
        routes.push({
          path: node.path,
          name: node.permission || node.path,
          component: () => import(/* @vite-ignore */ `@/${node.component}`),
          meta: { title: node.name, permission: node.permission, icon: node.icon }
        })
      }
      if (node.children && node.children.length) {
        walk(node.children)
      }
    })
  }
  walk(tree)
  return routes
}

/**
 * 将树形菜单转换成 el-menu 的 :default-active 可用数据
 * @param {Array} tree
 * @returns {Array} el-menu items
 */
export function toMenuItems(tree) {
  return tree.map((node) => ({
    index: node.path || String(node.id),
    title: node.name,
    icon: node.icon,
    type: node.type,
    children: node.children && node.children.length ? toMenuItems(node.children) : null
  }))
}
