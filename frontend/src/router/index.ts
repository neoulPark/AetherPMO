import { createRouter, createWebHashHistory } from 'vue-router'
import MainLayout from '@/components/layout/MainLayout.vue'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      component: MainLayout,
      children: [
        { path: '', redirect: '/projects/execution' },
        { path: 'projects/bidding', component: () => import('@/views/projects/BiddingList.vue') },
        { path: 'projects/bidding/:id', component: () => import('@/views/projects/BiddingDetail.vue') },
        { path: 'projects/:stage(execution|completed)', component: () => import('@/views/projects/ProjectList.vue') },
        {
          path: 'projects/:id(\\d+)',
          component: () => import('@/views/projects/ProjectDetail.vue'),
          children: [
            { path: '', redirect: 'overview' },
            { path: 'overview', component: () => import('@/views/projects/tabs/ProjectOverview.vue') },
            { path: 'deliverables', component: () => import('@/views/projects/tabs/ProjectDeliverables.vue') },
            { path: 'meetings', component: () => import('@/views/projects/tabs/ProjectMeetings.vue') },
            { path: 'risks', component: () => import('@/views/projects/tabs/ProjectRisks.vue') },
            { path: 'action-items', component: () => import('@/views/projects/tabs/ProjectActionItems.vue') },
            { path: 'official-docs', component: () => import('@/views/projects/tabs/ProjectOfficialDocs.vue') },
          ],
        },
        { path: 'templates', component: () => import('@/views/templates/TemplateList.vue') },
        { path: 'issues', component: () => import('@/views/issues/IssueList.vue') },
        { path: 'action-items', component: () => import('@/views/actionitems/ActionItemList.vue') },
        { path: 'official-docs', component: () => import('@/views/officialdocs/OfficialDocList.vue') },
        { path: 'meetings', component: () => import('@/views/meetings/MeetingList.vue') },
      ],
    },
  ],
})

export default router
