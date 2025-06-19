/**
 * 数据分析页面脚本
 */
$(document).ready(function() {
    // 初始化主题切换
    initTheme();
    
    // 初始化侧边栏
    initSidebar();
    
    // 检查用户登录状态
    checkLoginStatus();

    // 从URL中获取historyId参数
    getHistoryIdFromUrl();

    // 初始化所有图表
    initAllCharts();

    // 事件绑定
    bindEvents();
    
    // 加载历史记录
    loadHistoryList();
});

// 全局变量，存储从URL中获取的历史ID
let historyId = null;

/**
 * 初始化主题切换
 */
function initTheme() {
    // 获取当前主题
    const currentTheme = localStorage.getItem('theme') || 'light';
    document.documentElement.setAttribute('data-theme', currentTheme);
    
    // 设置主题切换按钮图标
    updateThemeIcon();
    
    // 主题切换事件
    $('#themeToggle').on('click', function() {
        const currentTheme = document.documentElement.getAttribute('data-theme');
        const newTheme = currentTheme === 'light' ? 'dark' : 'light';
        
        document.documentElement.setAttribute('data-theme', newTheme);
        localStorage.setItem('theme', newTheme);
        
        updateThemeIcon();
    });
}

/**
 * 更新主题图标
 */
function updateThemeIcon() {
    const currentTheme = document.documentElement.getAttribute('data-theme');
    const iconHtml = currentTheme === 'light' 
        ? '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z"></path></svg>'
        : '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z"></path></svg>';
    
    $('#themeToggle').html(iconHtml);
}

/**
 * 初始化侧边栏
 */
function initSidebar() {
    // 侧边栏折叠/展开
    $('#toggleSidebar').on('click', function() {
        $('#sidebar').toggleClass('collapsed');
    });
}

/**
 * 从URL中获取historyId参数
 */
function getHistoryIdFromUrl() {
    const urlParams = new URLSearchParams(window.location.search);
    historyId = urlParams.get('historyId');
    
    if (!historyId) {
        console.error("URL中缺少historyId参数，数据分析页面无法正常加载。");
        // 可以考虑重定向回主页或显示一个通用错误页面
        // window.location.href = 'index.html'; 
    }
    
    if (historyId) {
        // 如果有historyId参数，显示过滤提示
        $('#history-filter-info').removeClass('d-none').text(`当前正在分析爬取历史ID: ${historyId} 的数据`);
    } else {
        $('#history-filter-info').addClass('d-none');
    }
}

/**
 * 检查用户登录状态
 */
function checkLoginStatus() {
    $.ajax({
        url: '/api/user/current',
        type: 'GET',
        success: function(response) {
            if (response.username) {
                // 更新用户信息
                $('.user-info').show();
                $('.auth-buttons').hide();
                $('#username').text(response.username);
            } else {
                // 未登录，显示登录按钮
                $('.user-info').hide();
                $('.auth-buttons').show();
            }
        },
        error: function() {
            // 获取用户信息失败，显示登录按钮
            $('.user-info').hide();
            $('.auth-buttons').show();
        }
    });
}

/**
 * 加载历史记录列表
 */
function loadHistoryList() {
    $.ajax({
        url: '/api/history/recent?limit=5',
        type: 'GET',
        success: function(response) {
            const historyList = $('#historyList');
            historyList.empty();
            
            if (response && response.length > 0) {
                response.forEach(function(item) {
                    const historyItem = $('<li class="history-item"></li>');
                    const historyLink = $('<a href="#"></a>').text(item.title || `爬取记录 #${item.id}`);
                    
                    historyLink.on('click', function(e) {
                        e.preventDefault();
                        window.location.href = `analysis.html?historyId=${item.id}`;
                    });
                    
                    historyItem.append(historyLink);
                    historyList.append(historyItem);
                });
            } else {
                historyList.append('<li class="history-empty">暂无历史记录</li>');
            }
        },
        error: function() {
            const historyList = $('#historyList');
            historyList.empty();
            historyList.append('<li class="history-empty">加载历史失败</li>');
        }
    });
}

/**
 * 初始化所有图表
 */
function initAllCharts() {
    // 初始化各个图表
    loadWordCloud();
    loadHotWords();
    loadSourceDistribution();
    
    // 时间趋势图需要关键词，默认加载一个热门关键词
    loadInitialTrendKeyword();
}

/**
 * 绑定事件处理
 */
function bindEvents() {
    // 应用日期过滤器 (此按钮已移除，此处事件绑定不再需要)
    // $('#apply-date-filter').on('click', function() {
    //     refreshAllCharts();
    // });
    
    // 词云数据源切换 (已移除，词云固定分析关键词)
    // $('.word-cloud-source').on('click', function(e) {
    //     e.preventDefault();
    //     const source = $(this).data('source');
    //     $('#wordCloudSourceDropdown').text('数据源: ' + getSourceName(source));
    //     loadWordCloud(source);
    // });
    
    // 时间趋势查询
    $('#search-trend').on('click', function() {
        const keyword = $('#trend-keyword').val().trim();
        if (keyword) {
            loadTimeTrend(keyword);
        } else {
            showMessage('请输入关键词', 'warning');
        }
    });
    
    // 按回车键搜索趋势
    $('#trend-keyword').on('keypress', function(e) {
        if (e.which === 13) {
            $('#search-trend').click();
        }
    });
    
    // 退出登录
    $('#logout-btn').on('click', function() {
        $.ajax({
            url: '/api/user/logout',
            type: 'POST',
            success: function() {
                window.location.href = 'login.html';
            }
        });
    });
    
    // 清空历史记录
    $('#clearAllHistory').on('click', function() {
        if (confirm('确定要清空所有历史记录吗？')) {
            $.ajax({
                url: '/api/history/clear',
                type: 'DELETE',
                success: function() {
                    $('#historyList').empty().append('<li class="history-empty">暂无历史记录</li>');
                    // 清空所有图表并提示用户选择历史记录
                    historyId = null; // 重置historyId
                    $('#history-filter-info').addClass('d-none');
                    initAllCharts(); // 重新初始化图表，会显示空数据提示
                }
            });
        }
    });
}

/**
 * 刷新所有图表
 */
function refreshAllCharts() {
    loadWordCloud(); // 词云数据固定为关键词
    loadHotWords();
    loadSourceDistribution();
    
    // 重新加载热门关键词，确保时间趋势图更新
    loadInitialTrendKeyword();
}

/**
 * 获取数据源名称
 */
// function getSourceName(source) {
//     switch(source) {
//         case 'title': return '标题';
//         case 'content': return '正文';
//         case 'keywords': return '关键词';
//         default: return '标题';
//     }
// }

/**
 * 获取当前时间范围参数 (现在只返回historyId)
 */
function getTimeRangeParams() {
    const params = {};
    
    // 如果有历史ID，添加到参数中
    if (historyId) {
        params.historyId = historyId;
    }
    
    return params;
}

/**
 * 加载词云数据
 * 始终分析关键词数据，不再接收source参数
 */
function loadWordCloud() {
    if (!historyId) {
        showMessage('请先选择或进行一次爬取历史，然后查看数据分析', 'info');
        clearChart('wordCloudChart', '暂无词云数据，请选择历史记录');
        return;
    }

    const limit = $('#wordCloudLimit').val();
    const requestData = {
        historyId: parseInt(historyId),
        limit: parseInt(limit)
    };

    $.ajax({
        url: '/api/analysis/word-cloud',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(requestData),
        success: function(response) {
            if (response && response.wordCloud && response.wordCloud.length > 0) {
                renderWordCloud(response.wordCloud);
            } else {
                console.warn('词云数据为空或格式不正确');
                $('#word-cloud-empty').removeClass('d-none');
            }
        },
        error: function(xhr) {
            console.error('加载词云数据失败:', xhr.status, xhr.statusText);
            console.error('响应内容:', xhr.responseText);
            try {
                const errorResponse = JSON.parse(xhr.responseText);
                console.error('错误详情:', errorResponse);
            } catch (e) {
                console.error('无法解析错误响应');
            }
        }
    });
}

/**
 * 渲染词云
 */
function renderWordCloud(wordCloudData) {
    console.log('开始渲染词云, 数据条数:', wordCloudData.length);
    
    // 转换数据格式为wordcloud2.js所需的格式
    const list = wordCloudData.map(item => [item.text, item.weight]);
    
    console.log('词云数据前10项:', list.slice(0, 10));
    
    // 获取容器尺寸
    const container = document.getElementById('word-cloud-container');
    const width = container.offsetWidth;
    const height = container.offsetHeight || 300;
    
    console.log('词云容器尺寸:', width, 'x', height);
    
    // 获取主题颜色
    const isDarkTheme = document.documentElement.getAttribute('data-theme') === 'dark';
    
    try {
        // 清空容器
        while (container.firstChild) {
            container.removeChild(container.firstChild);
        }
        
        // 渲染词云
        WordCloud(container, { 
            list: list,
            gridSize: 8,
            weightFactor: function (size) {
                return Math.pow(size, 0.8) * width / 1000;
            },
            fontFamily: 'Microsoft YaHei, sans-serif',
            color: function() {
                // 根据主题使用不同的颜色范围
                if (isDarkTheme) {
                    return 'hsl(' + Math.floor(Math.random() * 360) + ', 70%, 60%)';
                } else {
                    return 'hsl(' + Math.floor(Math.random() * 360) + ', 70%, 50%)';
                }
            },
            hover: function(item) {
                if (item) {
                    console.log(item[0] + ': ' + item[1]);
                }
            },
            click: function(item) {
                if (item) {
                    $('#trend-keyword').val(item[0]);
                    loadTimeTrend(item[0]);
                }
            },
            classes: 'word-cloud-text',
            drawOutOfBound: false,
            shrinkToFit: true,
            minSize: 10
        });
        console.log('词云渲染完成');
    } catch (e) {
        console.error('词云渲染失败:', e);
        $('#word-cloud-empty').removeClass('d-none').html(`
            <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1">
                <circle cx="12" cy="12" r="10"></circle>
                <path d="M8 15s1.5 2 4 2 4-2 4-2"></path>
                <line x1="9" y1="9" x2="9.01" y2="9"></line>
                <line x1="15" y1="9" x2="15.01" y2="9"></line>
            </svg>
            <span>词云渲染失败: ${e.message}</span>
        `);
    }
}

/**
 * 加载热词排行榜数据并渲染
 */
function loadHotWords() {
    // 显示加载中，隐藏空数据提示
    $('#hot-words-loading').show();
    $('#hot-words-empty').addClass('d-none');
    
    // 准备请求参数
    const params = {
        limit: 20,
        ...getTimeRangeParams()
    };
    
    console.log('热词请求参数:', params);
    
    // 发送请求
    $.ajax({
        url: '/api/analysis/hot-words',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(params),
        success: function(response) {
            $('#hot-words-loading').hide();
            
            console.log('热词API响应:', response);
            
            if (response && response.hotWords && response.hotWords.length > 0) {
                console.log('热词数据项数:', response.hotWords.length);
                renderHotWords(response.hotWords);
            } else {
                console.warn('热词数据为空或格式不正确');
                $('#hot-words-empty').removeClass('d-none');
            }
        },
        error: function(xhr) {
            $('#hot-words-loading').hide();
            $('#hot-words-empty').removeClass('d-none');
            console.error('加载热词数据失败:', xhr.status, xhr.statusText);
            console.error('响应内容:', xhr.responseText);
            try {
                const errorResponse = JSON.parse(xhr.responseText);
                console.error('错误详情:', errorResponse);
            } catch (e) {
                console.error('无法解析错误响应');
            }
        }
    });
}

/**
 * 渲染热词排行榜
 */
function renderHotWords(hotWords) {
    console.log('开始渲染热词排行榜, 数据条数:', hotWords.length);
    
    try {
        // 初始化ECharts实例
        const chartContainer = document.getElementById('hot-words-chart');
        const chart = echarts.init(chartContainer);
        
        // 数据预处理，确保至少有数据
        if (hotWords.length === 0) {
            console.warn('热词数据为空，无法渲染');
            $('#hot-words-empty').removeClass('d-none');
            return;
        }
        
        // 限制显示数量，太多会挤在一起
        const displayCount = Math.min(hotWords.length, 15);
        hotWords = hotWords.slice(0, displayCount);
        
        // 转换数据格式
        const data = hotWords.map(item => ({
            name: item.word,
            value: item.count
        }));
        
        console.log('热词图表数据:', data);
        
        // 图表配置
        const option = {
            tooltip: {
                trigger: 'axis',
                axisPointer: {
                    type: 'shadow'
                }
            },
            grid: {
                left: '3%',
                right: '4%',
                bottom: '3%',
                containLabel: true
            },
            xAxis: {
                type: 'value',
                boundaryGap: [0, 0.01]
            },
            yAxis: {
                type: 'category',
                data: data.map(item => item.name),
                axisLabel: {
                    interval: 0,
                    rotate: 0
                }
            },
            series: [
                {
                    name: '出现次数',
                    type: 'bar',
                    data: data.map(item => item.value),
                    itemStyle: {
                        color: function(params) {
                            const colorList = [
                                '#c23531', '#2f4554', '#61a0a8', '#d48265', '#91c7ae',
                                '#749f83', '#ca8622', '#bda29a', '#6e7074', '#546570'
                            ];
                            return colorList[params.dataIndex % colorList.length];
                        }
                    }
                }
            ]
        };
        
        // 设置图表
        chart.setOption(option);
        console.log('热词排行榜渲染完成');
        
        // 窗口大小改变时，调整图表大小
        window.addEventListener('resize', function() {
            chart.resize();
        });
    } catch (e) {
        console.error('热词排行榜渲染失败:', e);
        $('#hot-words-empty').removeClass('d-none').html(`
            <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1">
                <circle cx="12" cy="12" r="10"></circle>
                <path d="M8 15s1.5 2 4 2 4-2 4-2"></path>
                <line x1="9" y1="9" x2="9.01" y2="9"></line>
                <line x1="15" y1="9" x2="15.01" y2="9"></line>
            </svg>
            <span>热词排行榜渲染失败: ${e.message}</span>
        `);
    }
}

/**
 * 初始化时间趋势分析的关键词
 */
function loadInitialTrendKeyword() {
    // 先尝试获取热词，用第一个热词作为默认关键词
    $.ajax({
        url: '/api/analysis/hot-words',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({
            limit: 1,
            ...getTimeRangeParams()
        }),
        success: function(response) {
            let keyword = '科技'; // 默认关键词
            
            if (response.hotWords && response.hotWords.length > 0) {
                keyword = response.hotWords[0].word;
            }
            
            $('#trend-keyword').val(keyword);
            loadTimeTrend(keyword);
        },
        error: function() {
            // 获取热词失败，使用默认关键词
            const keyword = '科技';
            $('#trend-keyword').val(keyword);
            loadTimeTrend(keyword);
        }
    });
}

/**
 * 加载时间趋势数据并渲染
 */
function loadTimeTrend(keyword) {
    // 显示加载中，隐藏空数据提示
    $('#time-trend-loading').show();
    $('#time-trend-empty').addClass('d-none');
    
    // 准备请求参数
    const params = {
        keyword: keyword,
        timeUnit: $('#time-unit').val(),
        ...getTimeRangeParams()
    };
    
    // 发送请求
    $.ajax({
        url: '/api/analysis/time-trend',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(params),
        success: function(response) {
            $('#time-trend-loading').hide();
            
            if (response.trendData && response.trendData.length > 0) {
                renderTimeTrend(response.trendData, keyword);
            } else {
                $('#time-trend-empty').removeClass('d-none');
            }
        },
        error: function(xhr) {
            $('#time-trend-loading').hide();
            $('#time-trend-empty').removeClass('d-none');
            console.error('加载时间趋势数据失败:', xhr.responseText);
        }
    });
}

/**
 * 渲染时间趋势图
 */
function renderTimeTrend(trendData, keyword) {
    // 初始化ECharts实例
    const chart = echarts.init(document.getElementById('time-trend-chart'));
    
    // 数据预处理
    const xAxisData = trendData.map(item => item.timePoint);
    const seriesData = trendData.map(item => item.count);
    
    // 图表配置
    const option = {
        title: {
            text: `关键词"${keyword}"的时间趋势`,
            textStyle: {
                fontSize: 14
            }
        },
        tooltip: {
            trigger: 'axis',
            axisPointer: {
                type: 'shadow'
            }
        },
        grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            containLabel: true
        },
        xAxis: {
            type: 'category',
            data: xAxisData,
            axisLabel: {
                interval: 0,
                rotate: 30
            }
        },
        yAxis: {
            type: 'value'
        },
        series: [
            {
                name: '出现次数',
                type: 'line',
                data: seriesData,
                markPoint: {
                    data: [
                        {type: 'max', name: '最大值'},
                        {type: 'min', name: '最小值'}
                    ]
                },
                markLine: {
                    data: [
                        {type: 'average', name: '平均值'}
                    ]
                },
                smooth: true,
                lineStyle: {
                    width: 3,
                    color: '#5470c6'
                },
                areaStyle: {
                    opacity: 0.3,
                    color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                        {offset: 0, color: 'rgba(84, 112, 198, 0.5)'},
                        {offset: 1, color: 'rgba(84, 112, 198, 0.1)'}
                    ])
                }
            }
        ]
    };
    
    // 设置图表
    chart.setOption(option);
    
    // 窗口大小改变时，调整图表大小
    window.addEventListener('resize', function() {
        chart.resize();
    });
}

/**
 * 加载来源分布数据并渲染
 */
function loadSourceDistribution() {
    // 显示加载中，隐藏空数据提示
    $('#source-loading').show();
    $('#source-empty').addClass('d-none');
    
    // 准备请求参数
    const params = {
        ...getTimeRangeParams()
    };
    
    // 发送请求
    $.ajax({
        url: '/api/analysis/source-distribution',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(params),
        success: function(response) {
            $('#source-loading').hide();
            
            if (response.sourceDistribution && response.sourceDistribution.length > 0) {
                renderSourceDistribution(response.sourceDistribution);
            } else {
                $('#source-empty').removeClass('d-none');
            }
        },
        error: function(xhr) {
            $('#source-loading').hide();
            $('#source-empty').removeClass('d-none');
            console.error('加载来源分布数据失败:', xhr.responseText);
        }
    });
}

/**
 * 渲染来源分布图
 */
function renderSourceDistribution(sourceData) {
    // 初始化ECharts实例
    const chart = echarts.init(document.getElementById('source-distribution-chart'));
    
    // 数据预处理
    const data = sourceData.map(item => ({
        name: item.source,
        value: item.count
    }));
    
    // 图表配置
    const option = {
        tooltip: {
            trigger: 'item',
            formatter: '{a} <br/>{b}: {c} ({d}%)'
        },
        legend: {
            orient: 'vertical',
            right: 10,
            top: 'center',
            type: 'scroll',
            formatter: function(name) {
                return name.length > 10 ? name.substring(0, 10) + '...' : name;
            }
        },
        series: [
            {
                name: '来源分布',
                type: 'pie',
                radius: ['40%', '70%'],
                avoidLabelOverlap: false,
                itemStyle: {
                    borderRadius: 10,
                    borderColor: '#fff',
                    borderWidth: 2
                },
                label: {
                    show: false,
                    position: 'center'
                },
                emphasis: {
                    label: {
                        show: true,
                        fontSize: '16',
                        fontWeight: 'bold'
                    }
                },
                labelLine: {
                    show: false
                },
                data: data
            }
        ]
    };
    
    // 设置图表
    chart.setOption(option);
    
    // 窗口大小改变时，调整图表大小
    window.addEventListener('resize', function() {
        chart.resize();
    });
}