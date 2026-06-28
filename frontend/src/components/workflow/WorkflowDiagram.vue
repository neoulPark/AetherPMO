<template>
  <div class="wf-diagram">
    <div v-if="!hasStatuses" class="wf-diagram-empty">표시할 상태가 없습니다.</div>
    <VueFlow
      v-else
      :nodes="nodes"
      :edges="edges"
      :fit-view-on-init="true"
      :min-zoom="0.2"
      :max-zoom="2"
      :nodes-draggable="true"
      :nodes-connectable="false"
      :elements-selectable="false"
      :default-edge-options="{ type: 'smoothstep' }"
      class="wf-flow"
    >
      <template #node-status="props">
        <div class="wf-node" :style="props.data.style">
          {{ props.data.label }}
        </div>
      </template>
      <template #node-terminal="props">
        <div class="wf-node-terminal">{{ props.data.label }}</div>
      </template>
      <Background :gap="16" pattern-color="#2a2d3e" />
      <Controls :show-interactive="false" />
    </VueFlow>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { VueFlow, MarkerType, Position, type Node, type Edge } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import dagre from 'dagre'
import type { Workflow } from '@/api/workflow'

const props = defineProps<{ workflow: Workflow | null }>()

const NODE_W = 150
const NODE_H = 44
const TERMINAL_SIZE = 56

const hasStatuses = computed(() => (props.workflow?.statuses?.length ?? 0) > 0)

function readableText(hex: string): string {
  const c = (hex || '').replace('#', '')
  if (c.length < 6) return '#fff'
  const r = parseInt(c.slice(0, 2), 16)
  const g = parseInt(c.slice(2, 4), 16)
  const b = parseInt(c.slice(4, 6), 16)
  const lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255
  return lum > 0.6 ? '#1a1a1a' : '#ffffff'
}

interface Built {
  nodes: Node[]
  edges: Edge[]
}

const built = computed<Built>(() => {
  const wf = props.workflow
  if (!wf || !wf.statuses.length) return { nodes: [], edges: [] }

  const statuses = [...wf.statuses].sort((a, b) => a.sortOrder - b.sortOrder)

  // initial status: isInitial, else lowest sortOrder
  const initial = statuses.find((s) => s.isInitial) ?? statuses[0]
  const finals = statuses.filter((s) => s.isFinal)

  const START_ID = '__start__'
  const END_ID = '__end__'

  // ---- dagre layout ----
  const g = new dagre.graphlib.Graph()
  g.setGraph({ rankdir: 'LR', ranksep: 70, nodesep: 30, marginx: 16, marginy: 16 })
  g.setDefaultEdgeLabel(() => ({}))

  g.setNode(START_ID, { width: TERMINAL_SIZE, height: TERMINAL_SIZE })
  for (const s of statuses) {
    g.setNode(String(s.statusId), { width: NODE_W, height: NODE_H })
  }
  if (finals.length) g.setNode(END_ID, { width: TERMINAL_SIZE, height: TERMINAL_SIZE })

  g.setEdge(START_ID, String(initial.statusId))
  for (const t of wf.transitions) {
    if (g.hasNode(String(t.fromStatusId)) && g.hasNode(String(t.toStatusId))) {
      g.setEdge(String(t.fromStatusId), String(t.toStatusId))
    }
  }
  for (const f of finals) {
    g.setEdge(String(f.statusId), END_ID)
  }

  dagre.layout(g)

  const toPos = (id: string, w: number, h: number) => {
    const n = g.node(id)
    return { x: (n?.x ?? 0) - w / 2, y: (n?.y ?? 0) - h / 2 }
  }

  const nodes: Node[] = []

  nodes.push({
    id: START_ID,
    type: 'terminal',
    position: toPos(START_ID, TERMINAL_SIZE, TERMINAL_SIZE),
    data: { label: '시작' },
    sourcePosition: Position.Right,
    targetPosition: Position.Left,
    draggable: false,
    selectable: false,
  })

  for (const s of statuses) {
    const bg = s.color || '#4a5568'
    const isFinal = s.isFinal
    nodes.push({
      id: String(s.statusId),
      type: 'status',
      position: toPos(String(s.statusId), NODE_W, NODE_H),
      data: {
        label: s.name,
        style: {
          background: bg,
          color: readableText(bg),
          boxShadow: isFinal ? '0 0 0 2px var(--color-success)' : 'none',
        },
      },
      sourcePosition: Position.Right,
      targetPosition: Position.Left,
    })
  }

  if (finals.length) {
    nodes.push({
      id: END_ID,
      type: 'terminal',
      position: toPos(END_ID, TERMINAL_SIZE, TERMINAL_SIZE),
      data: { label: '종료' },
      sourcePosition: Position.Right,
      targetPosition: Position.Left,
      draggable: false,
      selectable: false,
    })
  }

  const edgeStyle = { stroke: '#8892a4', strokeWidth: 1.5 }
  const marker = { type: MarkerType.ArrowClosed, color: '#8892a4', width: 18, height: 18 }

  const edges: Edge[] = []
  edges.push({
    id: `e-start`,
    source: START_ID,
    target: String(initial.statusId),
    markerEnd: marker,
    style: edgeStyle,
    animated: false,
  })

  for (const t of wf.transitions) {
    if (!g.hasNode(String(t.fromStatusId)) || !g.hasNode(String(t.toStatusId))) continue
    edges.push({
      id: `e-${t.transitionId}`,
      source: String(t.fromStatusId),
      target: String(t.toStatusId),
      label: t.name || undefined,
      markerEnd: marker,
      style: edgeStyle,
      labelStyle: { fill: '#8892a4', fontSize: '11px' },
      labelBgStyle: { fill: '#1e2130' },
    })
  }

  for (const f of finals) {
    edges.push({
      id: `e-end-${f.statusId}`,
      source: String(f.statusId),
      target: END_ID,
      markerEnd: marker,
      style: edgeStyle,
    })
  }

  return { nodes, edges }
})

const nodes = computed(() => built.value.nodes)
const edges = computed(() => built.value.edges)
</script>

<style scoped>
.wf-diagram {
  width: 100%;
  height: 320px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-surface);
  overflow: hidden;
}
.wf-diagram-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  font-size: 13px;
  color: var(--text-muted);
}
.wf-flow {
  width: 100%;
  height: 100%;
}
.wf-node {
  min-width: 110px;
  max-width: 150px;
  padding: 8px 12px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
  text-align: center;
  line-height: 1.2;
  white-space: normal;
  word-break: keep-all;
}
.wf-node-terminal {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--bg-surface2);
  border: 1px solid var(--border);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
}
</style>

<style>
/* Dark-theme overrides for Vue Flow (global, unscoped) */
.wf-flow .vue-flow__pane {
  background: var(--bg-surface);
}
.wf-flow .vue-flow__node {
  font-family: inherit;
}
.wf-flow .vue-flow__handle {
  opacity: 0;
  pointer-events: none;
}
.wf-flow .vue-flow__controls {
  box-shadow: none;
}
.wf-flow .vue-flow__controls-button {
  background: var(--bg-surface2);
  border-bottom: 1px solid var(--border);
  fill: var(--text-secondary);
}
.wf-flow .vue-flow__controls-button:hover {
  background: var(--border);
}
.wf-flow .vue-flow__edge-text {
  fill: var(--text-secondary);
}
</style>
