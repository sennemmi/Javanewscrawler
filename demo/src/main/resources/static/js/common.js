/**
 * 公共函数库
 */

/**
 * 检查用户登录状态并更新UI
 * @param {Function} onLoggedIn 登录成功时的回调函数
 * @param {Function} onNotLoggedIn 未登录时的回调函数
 */
function checkUserLoginStatus(onLoggedIn, onNotLoggedIn) {
    $.ajax({
        url: '/api/user/current',
        type: 'GET',
        success: function(response) {
            if (response && response.username) {
                // 更新用户信息
                $('.user-info').show();
                $('.auth-buttons').hide();
                $('#username').text(response.username);
                
                if (onLoggedIn && typeof onLoggedIn === 'function') {
                    onLoggedIn(response);
                }
            } else {
                // 未登录，显示登录按钮
                $('.user-info').hide();
                $('.auth-buttons').show();
                
                if (onNotLoggedIn && typeof onNotLoggedIn === 'function') {
                    onNotLoggedIn();
                }
            }
        },
        error: function() {
            // 获取用户信息失败，显示登录按钮
            $('.user-info').hide();
            $('.auth-buttons').show();
            
            if (onNotLoggedIn && typeof onNotLoggedIn === 'function') {
                onNotLoggedIn();
            }
        }
    });
}

/**
 * 初始化主题
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
    
    // 绑定退出登录事件
    $('#logout-btn').on('click', function() {
        $.ajax({
            url: '/api/user/logout',
            type: 'POST',
            success: function() {
                window.location.href = 'login.html';
            }
        });
    });
}

/**
 * 格式化日期时间
 * @param {Date|string} date 日期对象或日期字符串
 * @param {boolean} includeTime 是否包含时间
 * @returns {string} 格式化后的日期时间字符串
 */
function formatDateTime(date, includeTime = true) {
    if (!date) return '';
    
    const d = new Date(date);
    
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    
    if (!includeTime) {
        return `${year}-${month}-${day}`;
    }
    
    const hours = String(d.getHours()).padStart(2, '0');
    const minutes = String(d.getMinutes()).padStart(2, '0');
    
    return `${year}-${month}-${day} ${hours}:${minutes}`;
}

/**
 * 显示提示消息
 * @param {string} message 消息内容
 * @param {string} type 消息类型：success, error, warning, info
 * @param {number} duration 显示时长（毫秒）
 */
function showMessage(message, type = 'info', duration = 3000) {
    // 移除现有消息
    $('.message-container').remove();
    
    // 创建消息容器
    const messageContainer = $('<div class="message-container"></div>');
    const messageBox = $(`<div class="message message-${type}"></div>`).text(message);
    
    messageContainer.append(messageBox);
    $('body').append(messageContainer);
    
    // 显示消息
    setTimeout(() => {
        messageBox.addClass('show');
    }, 10);
    
    // 自动关闭
    setTimeout(() => {
        messageBox.removeClass('show');
        setTimeout(() => {
            messageContainer.remove();
        }, 300);
    }, duration);
}

// 页面加载完成后执行
$(document).ready(function() {
    // 初始化主题
    initTheme();
    
    // 初始化侧边栏
    initSidebar();
    
    // 检查用户登录状态
    checkUserLoginStatus();
}); 