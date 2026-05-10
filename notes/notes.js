document.addEventListener('DOMContentLoaded', function() {
  // 1. Theme Toggle
  const themeToggle = document.getElementById('theme-toggle');
  let isLight = localStorage.getItem('theme') === 'light';
  
  function updateTheme() {
    if (isLight) {
      document.body.classList.add('light-theme');
      if(themeToggle) themeToggle.textContent = '🌙';
    } else {
      document.body.classList.remove('light-theme');
      if(themeToggle) themeToggle.textContent = '🌞';
    }
  }
  updateTheme();
  
  if (themeToggle) {
    themeToggle.addEventListener('click', function() {
      isLight = !isLight;
      localStorage.setItem('theme', isLight ? 'light' : 'dark');
      updateTheme();
    });
  }

  // 2. Back to Top
  const backToTop = document.getElementById('back-to-top');
  if (backToTop) {
    window.addEventListener('scroll', function() {
      if (window.scrollY > 300) {
        backToTop.classList.add('visible');
      } else {
        backToTop.classList.remove('visible');
      }
    });
    backToTop.addEventListener('click', function() {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    });
  }

  // 3. Table of Contents (TOC)
  const article = document.querySelector('article');
  if (article) {
    const headings = article.querySelectorAll('h2, h3');
    if (headings.length > 0) {
      const tocContainer = document.createElement('div');
      tocContainer.className = 'toc-container';
      
      const tocTitle = document.createElement('div');
      tocTitle.className = 'toc-title';
      tocTitle.innerHTML = '📑 文章目录';
      tocContainer.appendChild(tocTitle);
      
      const tocList = document.createElement('ul');
      tocList.className = 'toc-list';
      
      headings.forEach((heading, index) => {
        if (!heading.id) {
          heading.id = 'heading-' + index;
        }
        
        const li = document.createElement('li');
        const a = document.createElement('a');
        a.href = '#' + heading.id;
        a.textContent = heading.textContent;
        
        if (heading.tagName.toLowerCase() === 'h3') {
          a.className = 'toc-h3';
        }
        
        li.appendChild(a);
        tocList.appendChild(li);
      });
      
      tocContainer.appendChild(tocList);
      document.body.appendChild(tocContainer);
      
      // Highlight active TOC item on scroll
      const tocLinks = tocList.querySelectorAll('a');
      window.addEventListener('scroll', () => {
        let current = '';
        headings.forEach(heading => {
          const headingTop = heading.getBoundingClientRect().top;
          if (headingTop < 150) {
            current = heading.id;
          }
        });
        
        tocLinks.forEach(link => {
          link.classList.remove('active');
          if (link.getAttribute('href') === '#' + current) {
            link.classList.add('active');
          }
        });
      });
    }
  }

  // 4. Copy Code Button
  const preBlocks = document.querySelectorAll('pre');
  preBlocks.forEach(pre => {
    const copyBtn = document.createElement('button');
    copyBtn.className = 'copy-btn';
    copyBtn.textContent = 'Copy';
    
    copyBtn.addEventListener('click', () => {
      const code = pre.querySelector('code');
      const textToCopy = code ? code.innerText : pre.innerText;
      
      navigator.clipboard.writeText(textToCopy).then(() => {
        copyBtn.textContent = '✅ 已复制';
        copyBtn.classList.add('copied');
        
        setTimeout(() => {
          copyBtn.textContent = 'Copy';
          copyBtn.classList.remove('copied');
        }, 2000);
      });
    });
    
    pre.appendChild(copyBtn);
  });
});
