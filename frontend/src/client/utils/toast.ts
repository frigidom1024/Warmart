/* ============================================
   暖 · Warmart — Toast
   ============================================ */

type ToastType = 'success' | 'error' | 'warning' | 'info'

const TOAST_CONTAINER_ID = 'wz-toast-container'

const ICONS: Record<ToastType, string> = {
  success: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><path d="M22 4 12 14.01l-3-3"/></svg>',
  error: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M15 9l-6 6M9 9l6 6"/></svg>',
  warning: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2 2 19h20L12 2z"/><path d="M12 9v4M12 17v.01"/></svg>',
  info: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8v.01"/></svg>',
}

function getContainer(): HTMLElement {
  let container = document.getElementById(TOAST_CONTAINER_ID)
  if (!container) {
    container = document.createElement('div')
    container.id = TOAST_CONTAINER_ID
    container.className = 'wz-toast-container'
    document.body.appendChild(container)
  }
  return container
}

export function showToast(message: string, type: ToastType = 'info', duration: number = 3000): void {
  const container = getContainer()

  const toast = document.createElement('div')
  toast.className = `wz-toast wz-toast--${type}`

  toast.innerHTML = `
    <span class="wz-toast__icon">${ICONS[type]}</span>
    <p class="wz-toast__msg">${message}</p>
  `

  container.appendChild(toast)

  requestAnimationFrame(() => {
    toast.classList.add('wz-toast--enter')
  })

  const close = () => dismiss(toast)
  toast.addEventListener('click', close)

  const progressBar = toast.querySelector('.wz-toast__progress-bar') as HTMLElement
  requestAnimationFrame(() => {
    progressBar.style.transition = `width ${duration}ms linear`
    progressBar.style.width = '0%'
  })

  ;(toast as any)._autoTimer = setTimeout(close, duration)
}

function dismiss(toast: HTMLElement) {
  if (toast.classList.contains('wz-toast--leave')) return
  toast.classList.remove('wz-toast--enter')
  toast.classList.add('wz-toast--leave')
  clearTimeout((toast as any)._autoTimer)
  setTimeout(() => {
    toast.remove()
    const container = document.getElementById(TOAST_CONTAINER_ID)
    if (container && !container.children.length) {
      container.remove()
    }
  }, 350)
}
