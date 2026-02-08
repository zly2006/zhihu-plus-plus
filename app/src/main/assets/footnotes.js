// noinspection ES6ConvertVarToLetConst

(function() {
    // 查找所有的脚注引用
    var references = document.querySelectorAll('sup[data-draft-type="reference"]');

    if (references.length === 0) {
        return; // 没有脚注，直接返回
    }
    
    // 创建脚注容器
    var footnotesContainer = document.createElement('div');
    footnotesContainer.id = 'zhihu-plus-footnotes';
    footnotesContainer.style.cssText = 'margin-top: 40px; padding-top: 20px; border-top: 1px solid #e0e0e0; font-size: 14px;';
    
    // 添加脚注标题
    var footnotesTitle = document.createElement('h4');
    footnotesTitle.textContent = '参考资料';
    footnotesTitle.style.cssText = 'font-size: 16px; font-weight: bold; margin-bottom: 15px; color: #333;';
    footnotesContainer.appendChild(footnotesTitle);
    
    // 创建脚注列表
    var footnotesList = document.createElement('ol');
    footnotesList.style.cssText = 'padding-left: 20px; line-height: 1.8;';
    
    // 处理每个脚注引用
    references.forEach(function(ref, index) {
        var numero = ref.getAttribute('data-numero') || (index + 1).toString();
        var text = ref.getAttribute('data-text') || '';
        var url = ref.getAttribute('data-url') || '';
        
        // 给脚注引用添加ID和样式
        ref.id = 'ref-' + numero;
        ref.style.cssText = 'cursor: pointer; color: #0066cc; text-decoration: none; font-size: 0.85em; vertical-align: super;';
        
        // 给脚注引用添加点击事件
        ref.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            var footnote = document.getElementById('footnote-' + numero);
            if (footnote) {
                footnote.scrollIntoView({ behavior: 'smooth', block: 'center' });
                // 高亮动画
                footnote.style.backgroundColor = '#fff3cd';
                setTimeout(function() {
                    footnote.style.transition = 'background-color 1s ease';
                    footnote.style.backgroundColor = '';
                }, 100);
            }
        });
        
        // 创建脚注列表项
        var footnoteItem = document.createElement('li');
        footnoteItem.id = 'footnote-' + numero;
        footnoteItem.style.cssText = 'margin-bottom: 10px; color: #555; transition: background-color 0.3s ease;';
        footnoteItem.value = numero;
        
        // 创建返回链接
        var backLink = document.createElement('a');
        backLink.textContent = '↩';
        backLink.href = '#ref-' + numero;
        backLink.style.cssText = 'margin-right: 8px; color: #0066cc; text-decoration: none; cursor: pointer;';
        backLink.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            var refElement = document.getElementById('ref-' + numero);
            if (refElement) {
                refElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
        });
        
        footnoteItem.appendChild(backLink);
        
        // 添加脚注文本
        if (url) {
            var link = document.createElement('a');
            link.textContent = text;
            link.href = url;
            link.target = '_blank';
            link.style.cssText = 'color: #0066cc; text-decoration: none; word-break: break-all;';
            footnoteItem.appendChild(link);
        } else {
            var textNode = document.createTextNode(text);
            footnoteItem.appendChild(textNode);
        }
        
        footnotesList.appendChild(footnoteItem);
    });
    
    footnotesContainer.appendChild(footnotesList);
    
    // 将脚注容器添加到文章末尾
    var articleContent = document.querySelector('.RichContent-inner') || 
                         document.querySelector('.Post-RichTextContainer') || 
                         document.body;
    articleContent.appendChild(footnotesContainer);
    
    // 暗色主题适配
    if (document.body.classList.contains('dark-theme')) {
        footnotesContainer.style.borderTopColor = '#444';
        footnotesTitle.style.color = '#ddd';
        footnotesList.querySelectorAll('li').forEach(function(item) {
            item.style.color = '#bbb';
        });
    }
})();
