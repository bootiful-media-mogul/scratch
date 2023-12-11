<template>
  <div :class="visibilityCss">
    <div class="sidebar-panel-top">
      <div class="visibility-controls">
        <a href="#" @click="hide" v-if="visible">close</a>
        <a href="#" @click="show" v-if="!visible">{{ title }}</a>
      </div>
    </div>
    <div class="sidebar-panel-content">
      <slot />
    </div>
    <div class="sidebar-panel-bottom"></div>
  </div>
</template>

<style>
.visibility-controls {
  text-transform: uppercase;
  font-weight: bold;
  font-size: smaller;
}

.visibility-controls a {
  color: white;
  font-size: small;
}

.sidebar-panel {
  overflow: hidden;
  border-radius: var(--gutter-space) 0 0 var(--gutter-space);
  padding: var(--gutter-space);
  background-color: white;
}

.sidebar-panel-hidden {
  background-color: black;
}

.sidebar-panel-visible .sidebar-panel-top {
  background-color: black;

  padding-bottom: calc(0.5 * var(--gutter-space));
  margin-left: calc(-1 * var(--gutter-space));
  margin-top: calc(-1 * var(--gutter-space));
  margin-right: calc(-1 * var(--gutter-space));
  padding-left: var(--gutter-space);
  padding-top: var(--gutter-space);
}

.sidebar-panel-visible .sidebar-panel-content {
  padding-top: var(--gutter-space);
}

/*

.sidebar-panel {
  !*  padding: calc(var(--gutter-space));
    padding-bottom: 0 ;
    border-radius: 10px 0 0  0px;*!
  right: 0;
  position: relative;

  margin-right: 0;
}

.sidebar-panel-top {
  padding: calc(var(--gutter-space));
  padding-top: 0;
  border-radius: 10px 0 0 0px;
  background-color: black;
}

.sidebar-panel-bottom {
  padding: calc(var(--gutter-space));
  padding-bottom: 0;
  height: 20px;
  border-radius: 10px 10px 10px 10px;
}
*/
.sidebar-panel a {
  text-decoration: none;
}

.sidebar-panel-visible {
  width: 300px;
}

.sidebar-panel-hidden {
  width: 20px;
}
</style>

<script lang="ts">
export default {
  data() {
    return {
      visible: true
    }
  },

  props: ['title'],
  methods: {
    hide() {
      this.visible = false
    },
    show() {
      this.visible = true
    }
  },
  computed: {
    visibilityCss() {
      return 'sidebar-panel ' + (this.visible ? 'sidebar-panel-visible' : 'sidebar-panel-hidden')
    }
  }
}
</script>
