// noinspection ES6ConvertVarToLetConst

(function() {
    function scrollToElement(element, back) {
        if (typeof AndroidInterface !== 'undefined' && AndroidInterface.scrollToHeight) {
            AndroidInterface.scrollToHeight(Math.round(element.getBoundingClientRect().bottom + (back ? 0 : 900)));
        } else {
            element.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
    }

    // 查找所有的脚注引用
    var references = document.querySelectorAll('sup[data-draft-type="reference"]');

    if (references.length === 0) {
        return; // 没有脚注，直接返回
    }

    // 创建脚注容器
    var footnotesContainer = document.createElement('div');
    footnotesContainer.id = 'zhihu-plus-footnotes';

    // 添加脚注标题
    var footnotesTitle = document.createElement('h4');
    footnotesTitle.textContent = '参考资料';
    footnotesContainer.appendChild(footnotesTitle);

    // 创建脚注列表
    var footnotesList = document.createElement('ol');

    // 处理每个脚注引用
    references.forEach(function(ref, index) {
        var numero = ref.getAttribute('data-numero') || (index + 1).toString();
        var text = ref.getAttribute('data-text') || '';
        var url = ref.getAttribute('data-url') || '';

        // 给脚注引用添加ID
        ref.id = 'ref-' + numero;

        // 给脚注引用添加点击事件
        ref.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            var footnote = document.getElementById('footnote-' + numero);
            if (footnote) {
                scrollToElement(footnote, false);
                // 高亮动画
                footnote.classList.add('footnote-highlight');
                setTimeout(function() {
                    footnote.classList.remove('footnote-highlight');
                }, 1100);
            }
        });

        // 创建脚注列表项
        var footnoteItem = document.createElement('li');
        footnoteItem.id = 'footnote-' + numero;
        footnoteItem.value = numero;

        // 创建返回链接
        var backLink = document.createElement('a');
        backLink.textContent = '↩';
        backLink.href = '#ref-' + numero;
        backLink.style.marginRight = '8px';
        backLink.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            var refElement = document.getElementById('ref-' + numero);
            if (refElement) {
                scrollToElement(refElement, true);
            }
        });

        footnoteItem.appendChild(backLink);

        // 添加脚注文本
        if (url) {
            var link = document.createElement('a');
            link.textContent = text || '打开链接';
            link.href = url;
            link.target = '_blank';
            link.className = 'footnote-url';
            footnoteItem.appendChild(link);
        } else {
            footnoteItem.appendChild(document.createTextNode(text));
        }

        footnotesList.appendChild(footnoteItem);
    });

    footnotesContainer.appendChild(footnotesList);

    // 将脚注容器添加到文章末尾
    var articleContent = document.querySelector('.RichContent-inner') ||
                         document.querySelector('.Post-RichTextContainer') ||
                         document.body;
    articleContent.appendChild(footnotesContainer);
})();
