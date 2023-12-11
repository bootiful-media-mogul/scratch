<template>


  <div :class="visibilityCss ">
    <div class="visibility-controls">
      <a href="#" @click="hide" v-if="visible">close</a>
      <a href="#" @click="show" v-if="!visible">{{ title }}</a>
    </div>
    <div class="content">
      <slot/>
    </div>
  </div>
</template>

<style>
.visibility-controls {
  text-transform: uppercase;
  font-weight: bold;
  font-size: smaller;

}

.sidebar-panel-visible .visibility-controls {
  padding-bottom: calc( 0.5 * var(--gutter-space)) ;
  padding-top : calc( -1 * var(--gutter-space))

}

.sidebar-panel-visible .content {
  background-color: white;
  margin-left: calc(-1 * var(--gutter-space));
  padding-left: var(--gutter-space);
  margin-right: calc(-1 * var(--gutter-space));
  padding-right: var(--gutter-space);
}

.visibility-controls a {
  color: white;
}

.sidebar-panel {
  padding: calc(var(--gutter-space));
  border-radius: 10px 0 0 10px;
  right: 0;
  position: relative;
  background-color: black;
  margin-right: 0;
  /*right: calc(-1 * var(--gutter-space)); */
}

.sidebar-panel a {
  text-decoration: none
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
      visible: true,
    }
  },

  props: ['title'],
  methods: {
    hide() {
      this.visible = false
    },
    show() {
      this.visible = true
    },
  },
  computed: {
    visibilityCss() {
      return 'sidebar-panel ' + (this.visible ? 'sidebar-panel-visible' : 'sidebar-panel-hidden')
    }
  }
}
</script>
