/**
 * ECharts 图表配置
 * Android 下载管理器 - 工程交付规约
 */
(function() {
  'use strict';

  function getThemeColors() {
    const isDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    return {
      textPrimary: isDark ? '#e6edf3' : '#1f2328',
      textSecondary: isDark ? '#8b949e' : '#656d76',
      bgCard: isDark ? '#1c2128' : '#ffffff',
      border: isDark ? '#30363d' : '#d0d7de',
      accent: isDark ? '#58a6ff' : '#0969da',
      green: isDark ? '#3fb950' : '#1a7f37',
      orange: isDark ? '#d2991d' : '#9a6700',
      red: isDark ? '#f85149' : '#cf222e',
      purple: isDark ? '#a371f7' : '#8250df',
      cyan: isDark ? '#39d2c0' : '#1b7c83'
    };
  }

  function commonOptions(colors) {
    return {
      backgroundColor: 'transparent',
      textStyle: { color: colors.textSecondary, fontFamily: 'InstrumentSans, sans-serif' },
      title: { textStyle: { color: colors.textPrimary } },
      legend: { textStyle: { color: colors.textSecondary } },
      tooltip: { backgroundColor: colors.bgCard, borderColor: colors.border, textStyle: { color: colors.textPrimary } }
    };
  }

  function initCharts() {
    var colors = getThemeColors();
    var base = commonOptions(colors);

    // Chart 1: 下载统计 - 分类分布饼图
    (function() {
      var dom = document.getElementById('chart-download-stats');
      if (!dom) return;
      var chart = echarts.init(dom);
      chart.setOption(Object.assign({}, base, {
        title: { text: '下载文件分类分布', left: 'center', top: 10 },
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { orient: 'horizontal', bottom: 10, left: 'center' },
        series: [{
          name: '分类',
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['50%', '50%'],
          avoidLabelOverlap: true,
          itemStyle: { borderRadius: 6, borderColor: colors.bgCard, borderWidth: 3 },
          label: { show: true, formatter: '{b}\n{d}%' },
          emphasis: { label: { fontSize: 16, fontWeight: 'bold' } },
          data: [
            { value: 245, name: '视频', itemStyle: { color: colors.accent } },
            { value: 178, name: '压缩包', itemStyle: { color: colors.orange } },
            { value: 156, name: '音频', itemStyle: { color: colors.green } },
            { value: 203, name: '安装包', itemStyle: { color: colors.purple } },
            { value: 89, name: '文档', itemStyle: { color: colors.cyan } },
            { value: 47, name: '其他', itemStyle: { color: colors.textSecondary } }
          ]
        }]
      }));
      window.addEventListener('resize', function() { chart.resize(); });
    })();

    // Chart 2: 下载速度分布
    (function() {
      var dom = document.getElementById('chart-speed-distribution');
      if (!dom) return;
      var chart = echarts.init(dom);
      chart.setOption(Object.assign({}, base, {
        title: { text: '下载速度分布与线程效率对比', left: 'center', top: 10 },
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        legend: { data: ['1线程', '3线程', '5线程', '9线程'], bottom: 10 },
        grid: { left: '3%', right: '4%', bottom: '15%', top: '18%', containLabel: true },
        xAxis: {
          type: 'category',
          data: ['<10MB', '10-50MB', '50-200MB', '200-500MB', '500MB-1GB', '>1GB'],
          axisLabel: { color: colors.textSecondary, fontSize: 11 },
          axisLine: { lineStyle: { color: colors.border } }
        },
        yAxis: {
          type: 'value',
          name: '平均速度 (MB/s)',
          nameTextStyle: { color: colors.textSecondary },
          axisLabel: { color: colors.textSecondary },
          splitLine: { lineStyle: { color: colors.border, type: 'dashed' } }
        },
        series: [
          {
            name: '1线程', type: 'bar',
            data: [5.2, 4.8, 4.5, 4.3, 4.0, 3.8],
            itemStyle: { color: colors.accent, borderRadius: [4, 4, 0, 0] }
          },
          {
            name: '3线程', type: 'bar',
            data: [8.5, 9.2, 10.5, 11.2, 10.8, 10.0],
            itemStyle: { color: colors.green, borderRadius: [4, 4, 0, 0] }
          },
          {
            name: '5线程', type: 'bar',
            data: [8.8, 10.5, 13.2, 14.8, 15.0, 14.2],
            itemStyle: { color: colors.purple, borderRadius: [4, 4, 0, 0] }
          },
          {
            name: '9线程', type: 'bar',
            data: [8.0, 10.0, 14.0, 16.5, 18.2, 17.5],
            itemStyle: { color: colors.orange, borderRadius: [4, 4, 0, 0] }
          }
        ]
      }));
      window.addEventListener('resize', function() { chart.resize(); });
    })();

    // Chart 3: 测试覆盖率雷达图
    (function() {
      var dom = document.getElementById('chart-coverage');
      if (!dom) return;
      var chart = echarts.init(dom);
      chart.setOption(Object.assign({}, base, {
        title: { text: '各模块单元测试覆盖率', left: 'center', top: 10 },
        tooltip: {},
        legend: { data: ['目标覆盖率', '实际覆盖率'], bottom: 10 },
        radar: {
          center: ['50%', '55%'],
          radius: '60%',
          indicator: [
            { name: '下载引擎', max: 100 },
            { name: '分类模块', max: 100 },
            { name: '加密模块', max: 100 },
            { name: '数据层', max: 100 },
            { name: 'Repository', max: 100 },
            { name: 'ViewModel', max: 100 }
          ],
          axisName: { color: colors.textSecondary },
          splitArea: {
            areaStyle: { color: [colors.bgCard, colors.bgCard] }
          }
        },
        series: [{
          name: '覆盖率',
          type: 'radar',
          data: [
            {
              value: [80, 80, 80, 80, 80, 80],
              name: '目标覆盖率',
              lineStyle: { color: colors.textSecondary, type: 'dashed' },
              areaStyle: { color: 'rgba(139, 148, 158, 0.1)' },
              itemStyle: { color: colors.textSecondary },
              symbol: 'none'
            },
            {
              value: [92, 88, 95, 85, 83, 78],
              name: '实际覆盖率',
              lineStyle: { color: colors.accent },
              areaStyle: { color: 'rgba(88, 166, 255, 0.2)' },
              itemStyle: { color: colors.accent }
            }
          ]
        }]
      }));
      window.addEventListener('resize', function() { chart.resize(); });
    })();

    // Chart 4: 测试用例执行状态
    (function() {
      var dom = document.getElementById('chart-test-status');
      if (!dom) return;
      var chart = echarts.init(dom);
      chart.setOption(Object.assign({}, base, {
        title: { text: '功能测试用例执行状态', left: 'center', top: 10 },
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { orient: 'horizontal', bottom: 10, left: 'center' },
        series: [{
          name: '测试状态',
          type: 'pie',
          radius: ['45%', '75%'],
          center: ['50%', '50%'],
          roseType: 'area',
          itemStyle: { borderRadius: 4, borderColor: colors.bgCard, borderWidth: 2 },
          label: { formatter: '{b}\n{c}个' },
          data: [
            { value: 28, name: '已通过', itemStyle: { color: colors.green } },
            { value: 3, name: '失败', itemStyle: { color: colors.red } },
            { value: 4, name: '待执行', itemStyle: { color: colors.orange } },
            { value: 0, name: '已跳过', itemStyle: { color: colors.textSecondary } }
          ]
        }]
      }));
      window.addEventListener('resize', function() { chart.resize(); });
    })();

    // Chart 5: 测试用例优先级分布
    (function() {
      var dom = document.getElementById('chart-test-priority');
      if (!dom) return;
      var chart = echarts.init(dom);
      chart.setOption(Object.assign({}, base, {
        title: { text: '测试用例优先级分布', left: 'center', top: 10 },
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: '3%', right: '4%', bottom: '10%', top: '18%', containLabel: true },
        xAxis: {
          type: 'category',
          data: ['下载引擎', '限速模块', '任务管理', '分类系统', '加密模块', '分享功能', '添加任务', '存储路径', '主题', '异常处理'],
          axisLabel: { color: colors.textSecondary, fontSize: 10, rotate: 30 },
          axisLine: { lineStyle: { color: colors.border } }
        },
        yAxis: {
          type: 'value',
          name: '用例数量',
          nameTextStyle: { color: colors.textSecondary },
          axisLabel: { color: colors.textSecondary },
          splitLine: { lineStyle: { color: colors.border, type: 'dashed' } }
        },
        series: [
          {
            name: 'P0 (高)', type: 'bar', stack: 'total',
            data: [5, 3, 4, 4, 3, 0, 2, 2, 0, 2],
            itemStyle: { color: colors.red },
            label: { show: true, position: 'inside', color: '#fff', fontSize: 10 }
          },
          {
            name: 'P1 (中)', type: 'bar', stack: 'total',
            data: [1, 1, 0, 2, 0, 2, 1, 1, 2, 2],
            itemStyle: { color: colors.orange },
            label: { show: true, position: 'inside', color: '#fff', fontSize: 10 }
          },
          {
            name: 'P2 (低)', type: 'bar', stack: 'total',
            data: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
            itemStyle: { color: colors.purple }
          }
        ]
      }));
      window.addEventListener('resize', function() { chart.resize(); });
    })();

    // Chart 6: 错误频率统计
    (function() {
      var dom = document.getElementById('chart-error-frequency');
      if (!dom) return;
      var chart = echarts.init(dom);
      chart.setOption(Object.assign({}, base, {
        title: { text: '常见错误发生频率统计', left: 'center', top: 10 },
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: '3%', right: '4%', bottom: '10%', top: '18%', containLabel: true },
        xAxis: {
          type: 'category',
          data: ['网络超时', '分块合并', '存储不足', '加密失败', '数据库异常', '并发异常', '权限错误', 'MD5校验'],
          axisLabel: { color: colors.textSecondary, fontSize: 10, rotate: 25 },
          axisLine: { lineStyle: { color: colors.border } }
        },
        yAxis: {
          type: 'value',
          name: '发生次数',
          nameTextStyle: { color: colors.textSecondary },
          axisLabel: { color: colors.textSecondary },
          splitLine: { lineStyle: { color: colors.border, type: 'dashed' } }
        },
        series: [{
          name: '错误频率',
          type: 'bar',
          data: [
            { value: 156, itemStyle: { color: colors.red } },
            { value: 89, itemStyle: { color: colors.orange } },
            { value: 67, itemStyle: { color: colors.orange } },
            { value: 45, itemStyle: { color: colors.accent } },
            { value: 34, itemStyle: { color: colors.purple } },
            { value: 28, itemStyle: { color: colors.cyan } },
            { value: 22, itemStyle: { color: colors.green } },
            { value: 15, itemStyle: { color: colors.textSecondary } }
          ],
          itemStyle: { borderRadius: [4, 4, 0, 0] },
          markLine: {
            data: [{ type: 'average', name: '平均值', lineStyle: { color: colors.accent, type: 'dashed' } }],
            label: { color: colors.accent, fontSize: 11 }
          }
        }]
      }));
      window.addEventListener('resize', function() { chart.resize(); });
    })();

    // Chart 7: 版本路线图
    (function() {
      var dom = document.getElementById('chart-version-roadmap');
      if (!dom) return;
      var chart = echarts.init(dom);
      chart.setOption(Object.assign({}, base, {
        title: { text: '版本路线图与发布计划', left: 'center', top: 10 },
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        legend: { data: ['新功能', 'Bug修复', '优化'], bottom: 10 },
        grid: { left: '3%', right: '4%', bottom: '15%', top: '18%', containLabel: true },
        xAxis: {
          type: 'category',
          data: ['v1.0.0\n2026-Q1', 'v1.0.1\n2026-Q2', 'v1.0.2\n2026-Q3', 'v1.1.0\n2026-Q4', 'v2.0.0\n2027-Q1'],
          axisLabel: { color: colors.textSecondary, fontSize: 10 },
          axisLine: { lineStyle: { color: colors.border } }
        },
        yAxis: {
          type: 'value',
          name: '任务数量',
          nameTextStyle: { color: colors.textSecondary },
          axisLabel: { color: colors.textSecondary },
          splitLine: { lineStyle: { color: colors.border, type: 'dashed' } }
        },
        series: [
          {
            name: '新功能', type: 'bar', stack: 'total',
            data: [14, 8, 6, 8, 12],
            itemStyle: { color: colors.accent, borderRadius: [4, 4, 0, 0] },
            label: { show: true, position: 'inside', color: '#fff', fontSize: 10 }
          },
          {
            name: 'Bug修复', type: 'bar', stack: 'total',
            data: [4, 6, 3, 2, 1],
            itemStyle: { color: colors.red, borderRadius: [0, 0, 0, 0] },
            label: { show: true, position: 'inside', color: '#fff', fontSize: 10 }
          },
          {
            name: '优化', type: 'bar', stack: 'total',
            data: [3, 4, 4, 5, 6],
            itemStyle: { color: colors.green, borderRadius: [0, 0, 4, 4] },
            label: { show: true, position: 'inside', color: '#fff', fontSize: 10 }
          }
        ]
      }));
      window.addEventListener('resize', function() { chart.resize(); });
    })();

    // Listen for theme changes
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function() {
      initCharts();
    });
  }

  // Initialize when DOM is ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initCharts);
  } else {
    initCharts();
  }
})();