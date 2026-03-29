/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { 
  Zap, 
  Cpu, 
  Database, 
  Thermometer, 
  Smartphone, 
  ShieldCheck, 
  Gamepad2, 
  Trash2, 
  Settings,
  Activity,
  LogOut,
  LogIn,
  Target,
  Crosshair,
  Sun,
  Moon,
  User as UserIcon,
  KeyRound,
  ExternalLink,
  AlertCircle,
  Check,
  X
} from 'lucide-react';
import { auth, logOut, onAuthStateChanged, User, db, signInWithGoogle } from './firebase';
import { getDoc, updateDoc, doc, setDoc, collection, onSnapshot, query, orderBy, addDoc, deleteDoc } from 'firebase/firestore';
import { Logo } from './components/Logo';

const CURRENT_VERSION_CODE = 1;

enum OperationType {
  CREATE = 'create',
  UPDATE = 'update',
  DELETE = 'delete',
  LIST = 'list',
  GET = 'get',
  WRITE = 'write',
}

interface FirestoreErrorInfo {
  error: string;
  operationType: OperationType;
  path: string | null;
  authInfo: {
    userId: string | undefined;
    email: string | null | undefined;
    emailVerified: boolean | undefined;
    isAnonymous: boolean | undefined;
    tenantId: string | null | undefined;
    providerInfo: {
      providerId: string;
      displayName: string | null;
      email: string | null;
      photoUrl: string | null;
    }[];
  }
}

function handleFirestoreError(error: unknown, operationType: OperationType, path: string | null) {
  const errInfo: FirestoreErrorInfo = {
    error: error instanceof Error ? error.message : String(error),
    authInfo: {
      userId: auth.currentUser?.uid,
      email: auth.currentUser?.email,
      emailVerified: auth.currentUser?.emailVerified,
      isAnonymous: auth.currentUser?.isAnonymous,
      tenantId: auth.currentUser?.tenantId,
      providerInfo: auth.currentUser?.providerData.map(provider => ({
        providerId: provider.providerId,
        displayName: provider.displayName,
        email: provider.email,
        photoUrl: provider.photoURL
      })) || []
    },
    operationType,
    path
  }
  console.error('Firestore Error: ', JSON.stringify(errInfo));
  throw new Error(JSON.stringify(errInfo));
}

export default function App() {
  const [user, setUser] = useState<User | null>(null);
  const [authLoading, setAuthLoading] = useState(true);
  const [username, setUsername] = useState('');
  const [otp, setOtp] = useState('');
  const [loginError, setLoginError] = useState('');
  const [isLoggingIn, setIsLoggingIn] = useState(false);
  const [sessionId, setSessionId] = useState<string | null>(localStorage.getItem('sessionId'));
  const [sessionStatus, setSessionStatus] = useState<'active' | 'expired' | null>(null);
  const [currentPage, setCurrentPage] = useState('dashboard');
  const [theme, setTheme] = useState<'dark' | 'light'>('dark');
  const [isBoosting, setIsBoosting] = useState(false);
  const [ramUsage, setRamUsage] = useState(64);
  const [cpuUsage, setCpuUsage] = useState(28);
  const [temp, setTemp] = useState(38);
  const [isRooted, setIsRooted] = useState(true);
  const [selectedFeature, setSelectedFeature] = useState('');
  const [updateInfo, setUpdateInfo] = useState<{ name: string; required: boolean; url: string } | null>(null);
  const [isCheckingUpdate, setIsCheckingUpdate] = useState(false);
  const isAdmin = user?.email === 'bpahan685@gmail.com';
  
  // Simulated Persistence
  const [featureStates, setFeatureStates] = useState<Record<string, boolean>>({
    'notifications': true,
    'auto_boost': false,
    'root_mode': true,
    'battery_saver': false,
    'game_mode': false,
    'network_optimizer': false,
    'display_tweaks': false,
    'kernel_tweaks': false,
    'system_debloater': false,
    'dns_changer': false,
    'charging_booster': false,
    'auto_clean': false,
    'fps_meter': false,
    'lag_fixer': false,
    'ff_optimizer': false,
    'ff_auto_boost': false,
    'ff_sensitivity': false,
    'fps_meter_enabled': false,
    'crosshair_enabled': false,
    'cool_down': true
  });

  const toggleFeature = (key: string) => {
    setFeatureStates(prev => ({ ...prev, [key]: !prev[key] }));
  };

  // Update Check
  useEffect(() => {
    const checkUpdate = async () => {
      try {
        const docRef = doc(db, 'app_config', 'version_info');
        const docSnap = await getDoc(docRef);
        
        if (docSnap.exists()) {
          const data = docSnap.data();
          const latestVersionCode = Number(data.latest_version_code) || 0;
          const latestVersionName = data.latest_version_name || '';
          const updateRequired = data.update_required || false;
          const updateUrl = data.update_url || '';

          console.log('Update check:', { latestVersionCode, CURRENT_VERSION_CODE });

          if (latestVersionCode > CURRENT_VERSION_CODE) {
            setUpdateInfo({
              name: latestVersionName,
              required: updateRequired,
              url: updateUrl
            });
          }
        }
      } catch (error) {
        console.error('Update check failed:', error);
      }
    };
    
    checkUpdate();
  }, []);

  const manualCheckUpdate = async () => {
    setIsCheckingUpdate(true);
    try {
      const docRef = doc(db, 'app_config', 'version_info');
      const docSnap = await getDoc(docRef);
      
      if (docSnap.exists()) {
        const data = docSnap.data();
        const latestVersionCode = Number(data.latest_version_code) || 0;
        const latestVersionName = data.latest_version_name || '';
        const updateRequired = data.update_required || false;
        const updateUrl = data.update_url || '';

        if (latestVersionCode > CURRENT_VERSION_CODE) {
          setUpdateInfo({
            name: latestVersionName,
            required: updateRequired,
            url: updateUrl
          });
        } else {
          alert('App is up to date!');
        }
      } else {
        alert('Update info not found in database.');
      }
    } catch (error) {
      console.error('Update check failed:', error);
      alert('Update check failed. Check console for details.');
    } finally {
      setIsCheckingUpdate(false);
    }
  };

  // Auth Listener
  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      setUser(currentUser);
      setAuthLoading(false);
    });
    return () => unsubscribe();
  }, []);

  // Session Monitor
  useEffect(() => {
    if (!sessionId) return;

    const sessionRef = doc(db, 'sessions', sessionId);
    const unsubscribe = onSnapshot(sessionRef, (docSnap) => {
      if (docSnap.exists()) {
        const data = docSnap.data();
        setSessionStatus(data.status);
        if (data.status === 'expired') {
          // Remote logout
          setUser(null);
          setSessionId(null);
          localStorage.removeItem('sessionId');
          setCurrentPage('login');
        }
      } else {
        // Session document deleted
        setUser(null);
        setSessionId(null);
        localStorage.removeItem('sessionId');
        setCurrentPage('login');
      }
    }, (error) => {
      handleFirestoreError(error, OperationType.GET, `sessions/${sessionId}`);
    });

    return () => unsubscribe();
  }, [sessionId]);

  // Simulate real-time updates
  useEffect(() => {
    const interval = setInterval(() => {
      setRamUsage(prev => Math.min(95, Math.max(40, prev + (Math.random() * 4 - 2))));
      setCpuUsage(prev => Math.min(90, Math.max(10, prev + (Math.random() * 10 - 5))));
      setTemp(prev => Math.min(55, Math.max(30, prev + (Math.random() * 2 - 1))));
    }, 2000);
    return () => clearInterval(interval);
  }, []);

  const handleLogin = async () => {
    if (!username.trim()) {
      setLoginError('Enter Username');
      return;
    }
    if (otp.length !== 6) {
      setLoginError('Enter 6-digit OTP');
      return;
    }

    setIsLoggingIn(true);
    setLoginError('');

    try {
      const otpDocRef = doc(db, 'otps', otp);
      let otpDocSnap;
      try {
        otpDocSnap = await getDoc(otpDocRef);
      } catch (error) {
        handleFirestoreError(error, OperationType.GET, `otps/${otp}`);
      }

      if (otpDocSnap && otpDocSnap.exists()) {
        const otpData = otpDocSnap.data();
        if (otpData && !otpData.isUsed) {
          try {
            await updateDoc(otpDocRef, { isUsed: true });
            
            // Create Session
            const sessionData = {
              userId: 'web-preview-user',
              username: username,
              status: 'active',
              createdAt: new Date().toISOString(),
              lastActive: new Date().toISOString()
            };
            
            const sessionCollection = collection(db, 'sessions');
            const sessionDocRef = await addDoc(sessionCollection, sessionData);
            const newSessionId = sessionDocRef.id;
            
            setSessionId(newSessionId);
            localStorage.setItem('sessionId', newSessionId);
            setSessionStatus('active');
          } catch (error) {
            console.error('Session creation failed:', error);
            handleFirestoreError(error, OperationType.WRITE, `sessions/new`);
          }
          // For web preview, we simulate a logged in user
          setUser({ uid: 'web-preview-user', email: 'ultrax@optimize.x', displayName: 'UltraX User' } as any);
        } else {
          setLoginError('OTP already used');
        }
      } else {
        setLoginError('Invalid OTP password');
      }
    } catch (error) {
      if (error instanceof Error && error.message.startsWith('{')) {
        // This is our structured error
        setLoginError('Permission Denied');
      } else {
        setLoginError('Connection Error');
      }
      console.error(error);
    } finally {
      setIsLoggingIn(false);
    }
  };

  const handleBoost = () => {
    setIsBoosting(true);
    setTimeout(() => {
      setIsBoosting(false);
      setRamUsage(42);
      setCpuUsage(15);
    }, 2000);
  };

  const handleLogout = async () => {
    if (sessionId) {
      try {
        const sessionRef = doc(db, 'sessions', sessionId);
        await updateDoc(sessionRef, { status: 'expired' });
      } catch (error) {
        console.error('Failed to expire session on logout:', error);
      }
    }
    await logOut();
    setUser(null);
    setCurrentPage('dashboard');
    setSessionId(null);
    localStorage.removeItem('sessionId');
  };

  const handleAdminLogin = async () => {
    try {
      setIsLoggingIn(true);
      const result = await signInWithGoogle();
      if (result.user.email === 'bpahan685@gmail.com') {
        // Create Session for Admin
        const sessionData = {
          userId: result.user.uid,
          username: result.user.displayName || 'Admin',
          status: 'active',
          createdAt: new Date().toISOString(),
          lastActive: new Date().toISOString()
        };
        const sessionCollection = collection(db, 'sessions');
        const sessionDocRef = await addDoc(sessionCollection, sessionData);
        setSessionId(sessionDocRef.id);
        localStorage.setItem('sessionId', sessionDocRef.id);
        setSessionStatus('active');
        setCurrentPage('dashboard');
      } else {
        await logOut();
        setLoginError('Access Denied: Admin Only');
      }
    } catch (error) {
      console.error('Admin login failed:', error);
      setLoginError('Admin Login Failed');
    } finally {
      setIsLoggingIn(false);
    }
  };

  const renderPage = () => {
    switch(currentPage) {
      case 'settings':
        return <SettingsPage 
          onBack={() => setCurrentPage('dashboard')} 
          states={featureStates} 
          onToggle={toggleFeature} 
          theme={theme}
          onToggleTheme={() => setTheme(prev => prev === 'dark' ? 'light' : 'dark')}
          onLogout={handleLogout}
          isAdmin={isAdmin}
          onOpenAdmin={() => setCurrentPage('admin')}
          onCheckUpdate={manualCheckUpdate}
          isCheckingUpdate={isCheckingUpdate}
        />;
      case 'admin':
        return <AdminDashboard onBack={() => setCurrentPage('settings')} theme={theme} />;
      case 'game-boost':
        return <GameBoostPage 
          onBack={() => setCurrentPage('dashboard')} 
          isActive={featureStates['game_mode']}
          onToggle={() => toggleFeature('game_mode')}
          theme={theme}
        />;
      case 'cpu-control':
        return <CpuControlPage onBack={() => setCurrentPage('dashboard')} theme={theme} />;
      case 'cleaner':
        return <CleanerPage onBack={() => setCurrentPage('dashboard')} theme={theme} />;
      case 'thermal':
        return <ThermalPage 
          onBack={() => setCurrentPage('dashboard')} 
          temp={temp} 
          isCoolDown={featureStates['cool_down']}
          onToggle={() => toggleFeature('cool_down')}
          theme={theme}
        />;
      case 'lag-fixer':
        return <LagFixerPage 
          onBack={() => setCurrentPage('dashboard')} 
          isEnabled={featureStates['lag_fixer']}
          onToggle={() => toggleFeature('lag_fixer')}
          theme={theme}
        />;
      case 'free-fire':
        return <FreeFirePage 
          onBack={() => setCurrentPage('dashboard')} 
          isEnabled={featureStates['ff_optimizer']}
          onToggle={() => toggleFeature('ff_optimizer')}
          autoBoost={featureStates['ff_auto_boost']}
          onToggleAuto={() => toggleFeature('ff_auto_boost')}
          sensitivity={featureStates['ff_sensitivity']}
          onToggleSensitivity={() => toggleFeature('ff_sensitivity')}
          theme={theme}
        />;
      case 'game-tools':
        return <GameToolsPage 
          onBack={() => setCurrentPage('dashboard')} 
          fpsEnabled={featureStates['fps_meter_enabled']}
          onToggleFps={() => toggleFeature('fps_meter_enabled')}
          crosshairEnabled={featureStates['crosshair_enabled']}
          onToggleCrosshair={() => toggleFeature('crosshair_enabled')}
          theme={theme}
        />;
      case 'generic':
        const key = selectedFeature.toLowerCase().replace(' ', '_');
        return <GenericFeaturePage 
          onBack={() => setCurrentPage('dashboard')} 
          title={selectedFeature} 
          isEnabled={featureStates[key] || false}
          onToggle={() => toggleFeature(key)}
          theme={theme}
        />;
      default:
        return (
          <div className="p-6 h-full flex flex-col relative overflow-hidden">
            <BackgroundEffects theme={theme} />
            
            {/* Header */}
            <div className="flex justify-between items-center mb-6 relative z-10">
              <div className="flex items-center gap-3">
                <Logo className="w-8 h-8" />
                <h1 className="text-xl font-black text-[#38BDF8] tracking-tight">
                  ULTRA OPTIMIZE <span className={theme === 'dark' ? 'text-white' : 'text-slate-900'}>X</span>
                </h1>
              </div>
              <div className="flex items-center gap-4">
                <motion.div
                  whileHover={{ scale: 1.1 }}
                  whileTap={{ scale: 0.9 }}
                  onClick={() => setTheme(prev => prev === 'dark' ? 'light' : 'dark')}
                  className="cursor-pointer"
                >
                  {theme === 'dark' ? (
                    <Sun className="w-5 h-5 text-yellow-400" />
                  ) : (
                    <Moon className="w-5 h-5 text-slate-400" />
                  )}
                </motion.div>
                <motion.div
                  whileHover={{ rotate: 90 }}
                  whileTap={{ scale: 0.9 }}
                  transition={{ type: "spring", stiffness: 200 }}
                >
                  <Settings 
                    className={`w-5 h-5 cursor-pointer transition-colors ${theme === 'dark' ? 'text-slate-400 hover:text-white' : 'text-slate-400 hover:text-slate-900'}`} 
                    onClick={() => setCurrentPage('settings')}
                  />
                </motion.div>
              </div>
            </div>

            {/* Device Info Card */}
            <motion.div 
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              className={`rounded-2xl p-5 mb-4 border relative overflow-hidden group transition-colors duration-500 ${theme === 'dark' ? 'bg-[#1E293B] border-white/5 shadow-2xl' : 'bg-white border-slate-100 shadow-[0_10px_30px_-10px_rgba(0,0,0,0.05)]'}`}
            >
              {/* Decorative line */}
              <div className="absolute top-0 left-0 w-1 h-full bg-[#38BDF8] opacity-50" />
              
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-3">
                  <div className="p-2 bg-[#38BDF8]/10 rounded-lg">
                    <Smartphone className="w-5 h-5 text-[#38BDF8]" />
                  </div>
                  <div>
                    <h3 className={`text-[10px] font-black uppercase tracking-widest ${theme === 'dark' ? 'text-slate-500' : 'text-slate-400'}`}>Device Model</h3>
                    <p className={`font-black text-sm tracking-tight ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>Samsung Galaxy S24 Ultra</p>
                  </div>
                </div>
                <motion.div 
                  animate={{ 
                    scale: [1, 1.05, 1],
                    opacity: [1, 0.8, 1]
                  }}
                  transition={{ repeat: Infinity, duration: 2 }}
                  className="flex items-center gap-1 bg-[#10B981]/10 px-2 py-1 rounded-md border border-[#10B981]/20"
                >
                  <ShieldCheck className="w-3 h-3 text-[#10B981]" />
                  <span className="text-[8px] font-black text-[#10B981] uppercase tracking-tighter">Secure</span>
                </motion.div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-3">
                  <div>
                    <p className={`text-[8px] font-black uppercase tracking-widest mb-1 ${theme === 'dark' ? 'text-slate-500' : 'text-slate-400'}`}>OS Version</p>
                    <p className={`text-[10px] font-bold ${theme === 'dark' ? 'text-slate-300' : 'text-slate-900'}`}>Android 14 (API 34)</p>
                  </div>
                  <div>
                    <p className={`text-[8px] font-black uppercase tracking-widest mb-1 ${theme === 'dark' ? 'text-slate-500' : 'text-slate-400'}`}>Processor</p>
                    <p className={`text-[10px] font-bold ${theme === 'dark' ? 'text-slate-300' : 'text-slate-900'}`}>Snapdragon 8 Gen 3</p>
                  </div>
                </div>
                <div className="space-y-3">
                  <div>
                    <p className={`text-[8px] font-black uppercase tracking-widest mb-1 ${theme === 'dark' ? 'text-slate-500' : 'text-slate-400'}`}>Storage Status</p>
                    <div className="flex items-end gap-1">
                      <p className={`text-[10px] font-bold ${theme === 'dark' ? 'text-slate-300' : 'text-slate-900'}`}>412 GB</p>
                      <p className="text-[8px] text-slate-500 mb-0.5">/ 512 GB</p>
                    </div>
                  </div>
                  <div>
                    <p className={`text-[8px] font-black uppercase tracking-widest mb-1 ${theme === 'dark' ? 'text-slate-500' : 'text-slate-400'}`}>Root Access</p>
                    <p className={`text-[10px] font-bold ${isRooted ? 'text-[#10B981]' : 'text-[#F43F5E]'}`}>
                      {isRooted ? 'AUTHORIZED' : 'DENIED'}
                    </p>
                  </div>
                </div>
              </div>
            </motion.div>

            {/* Stats Section */}
            <motion.div 
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 }}
              className={`rounded-[2.5rem] p-8 mb-6 border relative transition-colors duration-500 ${theme === 'dark' ? 'bg-[#1E293B] border-white/5 shadow-2xl' : 'bg-white border-slate-100 shadow-[0_20px_50px_-15px_rgba(0,0,0,0.05)]'}`}
            >
              {/* Grid background pattern */}
              <div className="absolute inset-0 opacity-[0.05] pointer-events-none" style={{ backgroundImage: `radial-gradient(${theme === 'dark' ? '#fff' : '#000'} 1px, transparent 1px)`, backgroundSize: '20px 20px' }} />
              
              <div className="flex justify-between items-center mb-8 relative z-10">
                <div className="flex flex-col">
                  <span className={`text-[10px] font-black uppercase tracking-[0.3em] ${theme === 'dark' ? 'text-slate-500' : 'text-slate-400'}`}>System Health</span>
                  <div className="flex items-center gap-2">
                    <div className="w-2 h-2 bg-[#10B981] rounded-full animate-pulse shadow-[0_0_8px_#10B981]" />
                    <span className={`text-xs font-black tracking-widest ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>OPTIMAL STATE</span>
                  </div>
                </div>
                <Activity className="w-5 h-5 text-[#38BDF8] opacity-50" />
              </div>
              
              <div className="flex justify-between items-center mb-10 relative z-10">
                <CircularMeter value={ramUsage} label="Memory" color="#38BDF8" theme={theme} />
                <CircularMeter value={cpuUsage} label="Processor" color="#EF4444" theme={theme} />
                <CircularMeter value={temp} label="Thermal" color="#F59E0B" unit="°C" theme={theme} />
              </div>

              <div className="grid grid-cols-2 gap-8 relative z-10">
                <div className={`border-l-2 pl-4 ${theme === 'dark' ? 'border-slate-800' : 'border-slate-100'}`}>
                  <p className={`text-[9px] font-black uppercase tracking-[0.2em] mb-1 ${theme === 'dark' ? 'text-slate-500' : 'text-slate-400'}`}>Latency</p>
                  <p className={`text-2xl font-black tracking-tighter ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>24<span className="text-sm text-slate-500 ml-1">ms</span></p>
                </div>
                <div className={`border-l-2 pl-4 ${theme === 'dark' ? 'border-slate-800' : 'border-slate-100'}`}>
                  <p className={`text-[9px] font-black uppercase tracking-[0.2em] mb-1 ${theme === 'dark' ? 'text-slate-500' : 'text-slate-400'}`}>Discharge</p>
                  <p className={`text-2xl font-black tracking-tighter ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>240<span className="text-sm text-slate-500 ml-1">mA</span></p>
                </div>
              </div>
            </motion.div>

            {/* Boost Button */}
            <motion.div 
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 0.2 }}
              className="relative mb-4"
            >
              <motion.button
                whileHover={{ scale: 1.02, boxShadow: '0 10px 30px rgba(56,189,248,0.3)' }}
                whileTap={{ scale: 0.98 }}
                onClick={handleBoost}
                disabled={isBoosting}
                className={`w-full py-3 rounded-xl font-black text-sm tracking-widest transition-all duration-300 flex items-center justify-center gap-2
                  ${isBoosting 
                    ? (theme === 'dark' ? 'bg-slate-800 text-slate-600' : 'bg-slate-100 text-slate-400')
                    : 'bg-[#38BDF8] text-white shadow-[0_10px_20px_rgba(56,189,248,0.2)]'
                  }`}
              >
                {isBoosting ? (
                  <motion.div
                    animate={{ rotate: 360 }}
                    transition={{ repeat: Infinity, duration: 1, ease: "linear" }}
                  >
                    <Activity className="w-5 h-5" />
                  </motion.div>
                ) : (
                  <>
                    <Zap className="w-5 h-5 fill-current" />
                    BOOST SYSTEM
                  </>
                )}
              </motion.button>
            </motion.div>

            {/* Quick Actions Grid - Scrollable */}
            <div className="flex-1 overflow-y-auto pr-1 custom-scrollbar">
              <motion.div 
                variants={{
                  hidden: { opacity: 0 },
                  show: {
                    opacity: 1,
                    transition: {
                      staggerChildren: 0.05,
                      delayChildren: 0.3
                    }
                  }
                }}
                initial="hidden"
                animate="show"
                className="grid grid-cols-2 gap-2 pb-4"
              >
                <QuickAction icon={<Gamepad2 />} label="Game Mode" onClick={() => setCurrentPage('game-boost')} theme={theme} />
                <QuickAction icon={<Cpu />} label="CPU Control" onClick={() => setCurrentPage('cpu-control')} theme={theme} />
                <QuickAction icon={<Trash2 />} label="Cleaner" onClick={() => setCurrentPage('cleaner')} theme={theme} />
                <QuickAction icon={<Activity />} label="Thermal" onClick={() => setCurrentPage('thermal')} theme={theme} />
                <QuickAction icon={<Zap />} label="Battery" onClick={() => { setSelectedFeature('Battery Saver'); setCurrentPage('generic'); }} theme={theme} />
                <QuickAction icon={<Database />} label="Apps" onClick={() => { setSelectedFeature('App Manager'); setCurrentPage('generic'); }} theme={theme} />
                <QuickAction icon={<Activity />} label="Network" onClick={() => { setSelectedFeature('Network Optimizer'); setCurrentPage('generic'); }} theme={theme} />
                <QuickAction icon={<Smartphone />} label="Display" onClick={() => { setSelectedFeature('Display Tweaks'); setCurrentPage('generic'); }} theme={theme} />
                <QuickAction icon={<Settings />} label="Kernel" onClick={() => { setSelectedFeature('Kernel Tweaks'); setCurrentPage('generic'); }} theme={theme} />
                <QuickAction icon={<Trash2 />} label="Debloat" onClick={() => { setSelectedFeature('System Debloater'); setCurrentPage('generic'); }} theme={theme} />
                <QuickAction icon={<Activity />} label="DNS" onClick={() => { setSelectedFeature('DNS Changer'); setCurrentPage('generic'); }} theme={theme} />
                <QuickAction icon={<Zap />} label="Charge" onClick={() => { setSelectedFeature('Charging Booster'); setCurrentPage('generic'); }} theme={theme} />
                <QuickAction icon={<Activity />} label="Auto" onClick={() => { setSelectedFeature('Auto Clean'); setCurrentPage('generic'); }} theme={theme} />
                <QuickAction icon={<Activity />} label="FPS" onClick={() => { setSelectedFeature('FPS Meter'); setCurrentPage('generic'); }} theme={theme} />
                <QuickAction icon={<Zap />} label="Lag Fix" onClick={() => setCurrentPage('lag-fixer')} theme={theme} />
                <QuickAction icon={<Gamepad2 />} label="Free Fire" onClick={() => setCurrentPage('free-fire')} theme={theme} />
                <QuickAction icon={<Activity />} label="Game Tools" onClick={() => setCurrentPage('game-tools')} theme={theme} />
                <QuickAction icon={<Settings />} label="Settings" onClick={() => setCurrentPage('settings')} theme={theme} />
              </motion.div>
            </div>
          </div>
        );
    }
  };

  return (
    <div className={`min-h-screen font-sans p-4 md:p-8 flex flex-col items-center transition-colors duration-500 selection:bg-[#38BDF8]/30 ${theme === 'dark' ? 'bg-[#0F172A] text-white' : 'bg-slate-50 text-slate-900'}`}>
      {/* Phone Frame Mockup */}
      <div className={`w-full max-w-md rounded-[3rem] border-8 shadow-[0_40px_100px_-20px_rgba(0,0,0,0.1)] overflow-hidden relative aspect-[9/19.5] transition-colors duration-500 ${theme === 'dark' ? 'bg-[#1E293B] border-[#334155]' : 'bg-white border-slate-200'}`}>
        
        {/* Status Bar */}
        <div className="h-8 bg-transparent flex justify-between items-center px-8 pt-2">
          <span className={`text-xs font-bold ${theme === 'dark' ? 'text-slate-500' : 'text-slate-400'}`}>9:41</span>
          <div className="flex gap-1 items-center">
            <div className={`w-4 h-4 rounded-full border ${theme === 'dark' ? 'border-slate-700' : 'border-slate-200'}`} />
            <div className={`w-4 h-4 rounded-full border ${theme === 'dark' ? 'border-slate-700' : 'border-slate-200'}`} />
          </div>
        </div>

        {/* FPS Overlay */}
        {featureStates['fps_meter_enabled'] && (
          <motion.div 
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            className={`absolute top-10 left-8 backdrop-blur-md px-2 py-1 rounded-md border shadow-sm z-50 ${theme === 'dark' ? 'bg-black/40 border-white/10' : 'bg-white/80 border-slate-100'}`}
          >
            <span className="text-[10px] font-black text-[#10B981]">60 FPS</span>
          </motion.div>
        )}

        {/* Crosshair Overlay */}
        {featureStates['crosshair_enabled'] && (
          <div className="absolute inset-0 flex items-center justify-center pointer-events-none z-50">
            <div className="w-4 h-4 border-2 border-[#EF4444] rounded-full relative">
              <div className="absolute inset-0 flex items-center justify-center">
                <div className="w-1 h-1 bg-[#EF4444] rounded-full" />
              </div>
            </div>
          </div>
        )}

        <AnimatePresence mode="wait">
          <motion.div
            key={currentPage}
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -20 }}
            transition={{ duration: 0.3, ease: "circOut" }}
            className="h-full"
          >
            {renderPage()}
          </motion.div>
        </AnimatePresence>

        {/* Home Indicator */}
        <div className={`absolute bottom-2 left-1/2 -translate-x-1/2 w-32 h-1 rounded-full ${theme === 'dark' ? 'bg-slate-800' : 'bg-slate-100'}`} />
      </div>

      <AnimatePresence>
        {!user && !authLoading && (
          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className={`fixed inset-0 z-[100] flex flex-col items-center justify-center p-12 overflow-hidden ${theme === 'dark' ? 'bg-[#0F172A]' : 'bg-white'}`}
          >
            {/* Background decorative elements */}
            <div className="absolute top-[-10%] left-[-10%] w-[60%] h-[60%] bg-[#38BDF8]/10 rounded-full blur-[120px]" />
            <div className="absolute bottom-[-10%] right-[-10%] w-[60%] h-[60%] bg-[#EF4444]/10 rounded-full blur-[120px]" />
            
            <div className="w-full max-w-md flex flex-col items-center relative z-10">
              <motion.div 
                initial={{ scale: 0.8, opacity: 0 }}
                animate={{ 
                  scale: 1, 
                  opacity: 1,
                  y: [0, -10, 0]
                }}
                transition={{ 
                  scale: { delay: 0.2, type: "spring" },
                  opacity: { delay: 0.2 },
                  y: { repeat: Infinity, duration: 3, ease: "easeInOut" }
                }}
                className="w-24 h-24 bg-gradient-to-br from-[#38BDF8] to-[#0EA5E9] rounded-[2.5rem] flex items-center justify-center mb-10 shadow-[0_20px_50px_rgba(56,189,248,0.2)] overflow-hidden"
              >
                <Logo className="w-16 h-16 object-contain" />
              </motion.div>
              
              <motion.div
                initial={{ y: 20, opacity: 0 }}
                animate={{ y: 0, opacity: 1 }}
                transition={{ delay: 0.4 }}
                className="text-center"
              >
                <h1 className={`text-5xl font-black mb-4 tracking-tighter leading-none ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>
                  ULTRA<br />
                  <span className="text-[#38BDF8]">OPTIMIZE</span> X
                </h1>
                <p className={`text-center mb-16 text-sm font-medium max-w-[280px] mx-auto leading-relaxed ${theme === 'dark' ? 'text-slate-400' : 'text-slate-500'}`}>
                  Professional-grade system calibration suite for advanced Android users.
                </p>
              </motion.div>
              
              <motion.div
                initial={{ y: 20, opacity: 0 }}
                animate={{ y: 0, opacity: 1 }}
                transition={{ delay: 0.6 }}
                className="w-full space-y-4"
              >
                <div className="relative">
                  <UserIcon className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                  <input
                    type="text"
                    placeholder="Username"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    className={`w-full pl-12 pr-4 py-4 rounded-2xl font-bold outline-none transition-all ${theme === 'dark' ? 'bg-slate-800/50 text-white focus:bg-slate-800' : 'bg-slate-100 text-slate-900 focus:bg-slate-200'}`}
                  />
                </div>
                
                <div className="relative">
                  <KeyRound className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                  <input
                    type="text"
                    placeholder="6-Digit OTP"
                    maxLength={6}
                    value={otp}
                    onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
                    className={`w-full pl-12 pr-4 py-4 rounded-2xl font-bold outline-none transition-all ${theme === 'dark' ? 'bg-slate-800/50 text-white focus:bg-slate-800' : 'bg-slate-100 text-slate-900 focus:bg-slate-200'}`}
                  />
                </div>

                {loginError && (
                  <motion.div 
                    initial={{ opacity: 0, x: -10 }}
                    animate={{ opacity: 1, x: 0 }}
                    className="flex items-center gap-2 text-red-500 text-xs font-bold px-2"
                  >
                    <AlertCircle className="w-4 h-4" />
                    {loginError}
                  </motion.div>
                )}

                <motion.button
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                  onClick={handleLogin}
                  disabled={isLoggingIn}
                  className={`w-full py-5 rounded-2xl font-black flex items-center justify-center gap-4 shadow-2xl tracking-tight transition-all ${isLoggingIn ? 'opacity-70 cursor-not-allowed' : ''} ${theme === 'dark' ? 'bg-[#38BDF8] text-white hover:bg-[#0EA5E9]' : 'bg-slate-900 text-white hover:bg-slate-800'}`}
                >
                  {isLoggingIn ? (
                    <Activity className="w-6 h-6 animate-spin" />
                  ) : (
                    <>
                      <LogIn className="w-6 h-6" />
                      AUTHENTICATE
                    </>
                  )}
                </motion.button>

                <div className="flex items-center gap-4 py-2">
                  <div className={`h-[1px] flex-1 ${theme === 'dark' ? 'bg-slate-800' : 'bg-slate-100'}`} />
                  <span className="text-[8px] font-black text-slate-500 uppercase tracking-widest">OR</span>
                  <div className={`h-[1px] flex-1 ${theme === 'dark' ? 'bg-slate-800' : 'bg-slate-100'}`} />
                </div>

                <motion.button
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                  onClick={handleAdminLogin}
                  disabled={isLoggingIn}
                  className={`w-full py-4 rounded-2xl font-black flex items-center justify-center gap-3 border transition-all ${theme === 'dark' ? 'bg-white/5 border-white/10 text-white hover:bg-white/10' : 'bg-white border-slate-200 text-slate-900 hover:bg-slate-50'}`}
                >
                  <ShieldCheck className="w-5 h-5 text-[#38BDF8]" />
                  <span className="text-xs uppercase tracking-widest">Admin Access</span>
                </motion.button>
              </motion.div>
              
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ delay: 0.8 }}
                className="mt-12 flex flex-col items-center gap-4"
              >
                <div className="flex gap-8">
                  <div className="flex flex-col items-center">
                    <span className={`font-black text-lg ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>500K+</span>
                    <span className="text-[8px] font-black text-slate-400 uppercase tracking-widest">Active Users</span>
                  </div>
                  <div className={`w-[1px] h-8 ${theme === 'dark' ? 'bg-slate-800' : 'bg-slate-100'}`} />
                  <div className="flex flex-col items-center">
                    <span className={`font-black text-lg ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>4.9/5</span>
                    <span className="text-[8px] font-black text-slate-400 uppercase tracking-widest">User Rating</span>
                  </div>
                </div>
                <p className="text-[9px] text-slate-400 text-center uppercase tracking-[0.2em] font-bold mt-4">
                  Secure OTP Authentication
                </p>
              </motion.div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {authLoading && (
        <div className={`fixed inset-0 z-[101] flex items-center justify-center ${theme === 'dark' ? 'bg-[#0F172A]' : 'bg-white'}`}>
          <motion.div
            animate={{ rotate: 360 }}
            transition={{ repeat: Infinity, duration: 1, ease: "linear" }}
          >
            <Activity className="w-10 h-10 text-[#38BDF8]" />
          </motion.div>
        </div>
      )}

      <AnimatePresence>
        {updateInfo && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-[200] flex items-center justify-center p-6 bg-black/60 backdrop-blur-sm"
          >
            <motion.div
              initial={{ scale: 0.9, y: 20 }}
              animate={{ scale: 1, y: 0 }}
              className={`w-full max-w-sm rounded-[2.5rem] p-8 border shadow-2xl relative overflow-hidden ${theme === 'dark' ? 'bg-[#1E293B] border-white/10' : 'bg-white border-slate-100'}`}
            >
              <div className="absolute top-0 left-0 w-full h-1 bg-[#38BDF8]" />
              
              <div className="flex flex-col items-center text-center">
                <div className="w-16 h-16 bg-[#38BDF8]/10 rounded-2xl flex items-center justify-center mb-6">
                  <Zap className="w-8 h-8 text-[#38BDF8]" />
                </div>
                
                <h2 className={`text-2xl font-black mb-2 tracking-tight ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>
                  {updateInfo.required ? 'Update Required' : 'Update Available'}
                </h2>
                
                <p className={`text-sm mb-8 leading-relaxed ${theme === 'dark' ? 'text-slate-400' : 'text-slate-500'}`}>
                  {updateInfo.required 
                    ? `A new version (${updateInfo.name}) is required to continue using Ultra Optimize X. Please update now.`
                    : `A new version (${updateInfo.name}) is available. Enhance your experience with the latest optimizations.`
                  }
                </p>
                
                <div className="w-full space-y-3">
                  <motion.button
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                    onClick={() => window.open(updateInfo.url, '_blank')}
                    className="w-full py-4 bg-[#38BDF8] text-white rounded-2xl font-black text-sm tracking-widest flex items-center justify-center gap-2 shadow-lg shadow-[#38BDF8]/20"
                  >
                    <ExternalLink className="w-4 h-4" />
                    UPDATE NOW
                  </motion.button>
                  
                  {!updateInfo.required && (
                    <motion.button
                      whileHover={{ scale: 1.02 }}
                      whileTap={{ scale: 0.98 }}
                      onClick={() => setUpdateInfo(null)}
                      className={`w-full py-4 rounded-2xl font-black text-xs tracking-widest transition-colors ${theme === 'dark' ? 'bg-white/5 text-slate-400 hover:bg-white/10' : 'bg-slate-100 text-slate-500 hover:bg-slate-200'}`}
                    >
                      LATER
                    </motion.button>
                  )}
                </div>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      <div className="mt-8 text-center max-w-md">
        <p className="text-slate-400 text-sm">
          This is a web preview of the <span className="text-[#38BDF8] font-semibold">Ultra Optimize X</span> Android application. 
          The full Kotlin source code and Gradle build system are available in the project files.
        </p>
      </div>
    </div>
  );
}

function SettingsPage({ onBack, states, onToggle, theme, onToggleTheme, onLogout, isAdmin, onOpenAdmin, onCheckUpdate, isCheckingUpdate }: { 
  onBack: () => void, 
  states: any, 
  onToggle: (k: string) => void, 
  theme: string, 
  onToggleTheme: () => void, 
  onLogout: () => void,
  isAdmin: boolean,
  onOpenAdmin: () => void,
  onCheckUpdate: () => void,
  isCheckingUpdate: boolean
}) {
  return (
    <motion.div 
      initial={{ opacity: 0, x: 50 }}
      animate={{ opacity: 1, x: 0 }}
      className="p-6 h-full flex flex-col"
    >
      <div className="flex items-center gap-4 mb-8">
        <Smartphone className="w-6 h-6 text-[#38BDF8] rotate-180 cursor-pointer" onClick={onBack} />
        <h2 className={`text-xl font-black tracking-tighter uppercase ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>Settings</h2>
      </div>
      <div className="space-y-4 flex-1">
        {isAdmin && (
          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={onOpenAdmin}
            className={`w-full p-5 rounded-2xl border flex items-center gap-4 transition-all ${theme === 'dark' ? 'bg-[#38BDF8]/10 border-[#38BDF8]/30 text-[#38BDF8]' : 'bg-[#38BDF8]/5 border-[#38BDF8]/20 text-[#38BDF8]'}`}
          >
            <ShieldCheck className="w-6 h-6" />
            <div className="flex flex-col items-start">
              <span className="text-sm font-bold uppercase tracking-tight">Admin Control</span>
              <span className="text-[10px] opacity-60">Manage User Sessions</span>
            </div>
          </motion.button>
        )}
        <div className={`rounded-2xl p-5 flex justify-between items-center border transition-colors ${theme === 'dark' ? 'bg-[#1E293B] border-white/5' : 'bg-slate-50 border-slate-100 shadow-sm'}`}>
          <div className="flex flex-col">
            <span className={`text-sm font-bold ${theme === 'dark' ? 'text-slate-200' : 'text-slate-700'}`}>Dark Mode</span>
            <span className="text-[10px] text-slate-500 uppercase font-bold">System Theme</span>
          </div>
          <div 
            onClick={onToggleTheme}
            className={`w-12 h-6 rounded-full relative cursor-pointer transition-all duration-300 ${theme === 'dark' ? 'bg-[#38BDF8]' : 'bg-slate-200'}`}
          >
            <motion.div 
              animate={{ x: theme === 'dark' ? 26 : 4 }} 
              className="absolute top-1 w-4 h-4 bg-white rounded-full shadow-md" 
            />
          </div>
        </div>

        {[
          { label: 'Enable Notifications', key: 'notifications' },
          { label: 'Auto Boost on Startup', key: 'auto_boost' },
          { label: 'Force Root Mode', key: 'root_mode' }
        ].map(item => (
          <div key={item.key} className={`rounded-2xl p-5 flex justify-between items-center border transition-colors ${theme === 'dark' ? 'bg-[#1E293B] border-white/5' : 'bg-slate-50 border-slate-100 shadow-sm'}`}>
            <span className={`text-sm font-bold ${theme === 'dark' ? 'text-slate-200' : 'text-slate-700'}`}>{item.label}</span>
            <div 
              onClick={() => onToggle(item.key)}
              className={`w-12 h-6 rounded-full relative cursor-pointer transition-all duration-300 ${states[item.key] ? 'bg-[#38BDF8]' : 'bg-slate-200'}`}
            >
              <motion.div 
                animate={{ x: states[item.key] ? 26 : 4 }} 
                className="absolute top-1 w-4 h-4 bg-white rounded-full shadow-md" 
              />
            </div>
          </div>
        ))}
      </div>

        <motion.button
          whileHover={{ scale: 1.02, backgroundColor: theme === 'dark' ? 'rgba(244, 63, 94, 0.2)' : 'rgba(244, 63, 94, 0.1)' }}
          whileTap={{ scale: 0.98 }}
          onClick={onLogout}
          className={`w-full py-4 rounded-2xl font-black text-xs tracking-widest flex items-center justify-center gap-2 border mt-auto transition-colors ${theme === 'dark' ? 'bg-[#1E293B] border-white/5 text-[#F43F5E]' : 'bg-slate-50 border-slate-100 text-[#F43F5E] shadow-sm'}`}
        >
          <LogOut className="w-4 h-4" />
          LOGOUT ACCOUNT
        </motion.button>

        <motion.button
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
          onClick={onCheckUpdate}
          disabled={isCheckingUpdate}
          className={`w-full py-4 rounded-2xl font-black text-[10px] tracking-widest flex items-center justify-center gap-2 border mt-4 transition-colors ${theme === 'dark' ? 'bg-white/5 border-white/10 text-slate-400 hover:text-white' : 'bg-white border-slate-200 text-slate-500 hover:text-slate-900 shadow-sm'}`}
        >
          <Zap className={`w-3.5 h-3.5 ${isCheckingUpdate ? 'animate-pulse' : ''}`} />
          {isCheckingUpdate ? 'CHECKING...' : 'CHECK FOR UPDATES'}
        </motion.button>
    </motion.div>
  );
}

function GameBoostPage({ onBack, isActive, onToggle, theme }: { onBack: () => void, isActive: boolean, onToggle: () => void, theme: string }) {
  return (
    <motion.div 
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      className="p-6 h-full flex flex-col items-center relative"
    >
      <BackgroundEffects theme={theme} />
      
      <div className="flex items-center gap-4 mb-8 w-full relative z-10">
        <Smartphone className="w-6 h-6 text-[#38BDF8] rotate-180 cursor-pointer" onClick={onBack} />
        <h2 className={`text-xl font-black tracking-tighter uppercase ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>Game Engine</h2>
      </div>
      
      <div className={`border rounded-[2.5rem] p-10 w-full flex flex-col items-center mb-8 relative overflow-hidden transition-colors ${theme === 'dark' ? 'bg-[#1E293B] border-white/5 shadow-2xl' : 'bg-white border-slate-100 shadow-[0_20px_50px_-15px_rgba(0,0,0,0.05)]'}`}>
        <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-transparent via-[#38BDF8] to-transparent opacity-30" />
        
        <div className={`relative p-8 rounded-full mb-6 ${isActive ? 'bg-[#10B981]/10' : 'bg-[#38BDF8]/10'}`}>
          <div className={`absolute inset-0 rounded-full blur-2xl opacity-20 ${isActive ? 'bg-[#10B981]' : 'bg-[#38BDF8]'}`} />
          <Gamepad2 className={`w-16 h-16 relative z-10 ${isActive ? 'text-[#10B981]' : 'text-[#38BDF8]'}`} />
        </div>

        <span className="text-[10px] font-black text-slate-500 uppercase tracking-[0.4em] mb-2">Engine Status</span>
        <span className={`text-2xl font-black mb-6 tracking-tighter ${isActive ? 'text-[#10B981]' : (theme === 'dark' ? 'text-white' : 'text-slate-900')}`}>
          {isActive ? 'SYNCHRONIZED' : 'STANDBY'}
        </span>
        
        <div className="grid grid-cols-2 gap-4 w-full">
          <div className={`p-4 rounded-2xl border transition-colors ${theme === 'dark' ? 'bg-black/20 border-white/5' : 'bg-slate-50 border-slate-100'}`}>
            <p className="text-[8px] font-black text-slate-500 uppercase mb-1">Priority</p>
            <p className={`text-xs font-black ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>HIGH-LEVEL</p>
          </div>
          <div className={`p-4 rounded-2xl border transition-colors ${theme === 'dark' ? 'bg-black/20 border-white/5' : 'bg-slate-50 border-slate-100'}`}>
            <p className="text-[8px] font-black text-slate-500 uppercase mb-1">Latency</p>
            <p className={`text-xs font-black ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>ULTRA-LOW</p>
          </div>
        </div>
      </div>

      <motion.button 
        whileHover={{ scale: 1.02, boxShadow: '0 20px 40px rgba(0,0,0,0.05)' }}
        whileTap={{ scale: 0.98 }}
        onClick={onToggle}
        className={`w-full py-5 rounded-2xl font-black tracking-widest transition-all shadow-xl ${isActive ? (theme === 'dark' ? 'bg-slate-800 text-slate-600' : 'bg-slate-100 text-slate-400') : (theme === 'dark' ? 'bg-white text-slate-900' : 'bg-slate-900 text-white')}`}
      >
        {isActive ? 'TERMINATE SESSION' : 'INITIALIZE ENGINE'}
      </motion.button>
    </motion.div>
  );
}

function CpuControlPage({ onBack, theme }: { onBack: () => void, theme: string }) {
  return (
    <motion.div 
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="p-6 h-full flex flex-col relative"
    >
      <BackgroundEffects theme={theme} />
      
      <div className="flex items-center gap-4 mb-8 relative z-10">
        <Smartphone className="w-6 h-6 text-[#38BDF8] rotate-180 cursor-pointer" onClick={onBack} />
        <h2 className={`text-xl font-black tracking-tighter uppercase ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>Processor Lab</h2>
      </div>
      
      <div className={`border rounded-3xl p-6 mb-6 relative overflow-hidden transition-colors ${theme === 'dark' ? 'bg-[#1E293B] border-white/5 shadow-2xl' : 'bg-white border-slate-100 shadow-sm'}`}>
        <div className="absolute top-0 right-0 p-4 opacity-5">
          <Cpu size={80} className={theme === 'dark' ? 'text-white' : 'text-slate-900'} />
        </div>
        <div className="relative z-10">
          <h3 className="text-[10px] font-black text-slate-500 uppercase tracking-[0.3em] mb-2">Active Silicon</h3>
          <p className={`font-black text-lg mb-1 ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>Snapdragon 8 Gen 3</p>
          <p className="text-[10px] font-bold text-[#38BDF8] tracking-widest">8-CORE ARCHITECTURE</p>
          
          <div className="mt-6 flex gap-2">
            {[1, 2, 3, 4, 5, 6, 7, 8].map(i => (
              <div key={i} className={`flex-1 h-8 rounded-md border transition-colors ${theme === 'dark' ? 'bg-black/20 border-white/5' : 'bg-slate-50 border-slate-100'} flex items-center justify-center`}>
                <div className="w-1 h-4 bg-[#38BDF8] rounded-full animate-pulse" style={{ animationDelay: `${i * 0.1}s` }} />
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="text-[10px] font-black text-slate-500 uppercase tracking-[0.3em] mb-4 ml-2">Governor Profiles</div>
      <div className="space-y-3">
        {['performance', 'powersave', 'schedutil', 'ondemand'].map(gov => (
          <motion.div 
            key={gov} 
            whileHover={{ backgroundColor: theme === 'dark' ? 'rgba(255,255,255,0.02)' : 'rgba(0,0,0,0.02)', scale: 1.01 }}
            className={`rounded-2xl p-5 border flex justify-between items-center group cursor-pointer transition-all ${theme === 'dark' ? 'bg-[#1E293B] border-white/5 shadow-2xl' : 'bg-white border-slate-100 shadow-sm'}`}
          >
            <div className="flex flex-col">
              <span className={`text-xs font-black uppercase tracking-widest group-hover:text-[#38BDF8] transition-colors ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>{gov}</span>
              <span className="text-[8px] text-slate-500 uppercase font-bold">
                {gov === 'performance' ? 'Maximum Clock Speed' : gov === 'powersave' ? 'Battery Efficiency' : 'Balanced Logic'}
              </span>
            </div>
            {gov === 'schedutil' ? (
              <div className="bg-[#38BDF8]/10 px-3 py-1 rounded-full border border-[#38BDF8]/20">
                <span className="text-[8px] font-black text-[#38BDF8] uppercase tracking-widest">Active</span>
              </div>
            ) : (
              <div className={`w-6 h-6 rounded-full border ${theme === 'dark' ? 'border-slate-700' : 'border-slate-100'}`} />
            )}
          </motion.div>
        ))}
      </div>
    </motion.div>
  );
}

function CleanerPage({ onBack, theme }: { onBack: () => void, theme: string }) {
  const [isCleaning, setIsCleaning] = useState(false);
  const [junkSize, setJunkSize] = useState(1.2);
  
  const handleClean = () => {
    setIsCleaning(true);
    setTimeout(() => {
      setIsCleaning(false);
      setJunkSize(0);
    }, 2500);
  };

  return (
    <motion.div 
      initial={{ opacity: 0, x: 50 }}
      animate={{ opacity: 1, x: 0 }}
      className="p-6 h-full flex flex-col items-center relative"
    >
      <BackgroundEffects theme={theme} />
      
      <div className="flex items-center gap-4 mb-8 w-full relative z-10">
        <Smartphone className="w-6 h-6 text-[#38BDF8] rotate-180 cursor-pointer" onClick={onBack} />
        <h2 className={`text-xl font-black tracking-tighter uppercase ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>Scrub Unit</h2>
      </div>
      
      <div className={`border rounded-[2.5rem] p-10 w-full flex flex-col items-center mb-8 relative transition-colors ${theme === 'dark' ? 'bg-[#1E293B] border-white/5 shadow-2xl' : 'bg-white border-slate-100 shadow-[0_20px_50px_-15px_rgba(0,0,0,0.05)]'}`}>
        <div className="absolute top-4 right-4 animate-spin-slow opacity-10">
          <Settings size={40} className={theme === 'dark' ? 'text-white' : 'text-[#F43F5E]'} />
        </div>
        
        <motion.div 
          animate={isCleaning ? { rotate: 360 } : {}}
          transition={{ repeat: Infinity, duration: 2, ease: "linear" }}
          className={`relative p-8 rounded-full mb-6 ${isCleaning ? 'bg-[#38BDF8]/20' : 'bg-[#38BDF8]/10'}`}
        >
          <Trash2 className="w-16 h-16 text-[#38BDF8] relative z-10" />
          {isCleaning && (
            <motion.div 
              initial={{ scale: 0.8, opacity: 0 }}
              animate={{ scale: 1.2, opacity: 1 }}
              className="absolute inset-0 border-2 border-[#38BDF8] rounded-full"
            />
          )}
        </motion.div>
        
        <span className="text-[10px] font-black text-slate-500 uppercase tracking-[0.4em] mb-4">Redundant Data</span>
        <div className="relative mb-8">
          <div className={`absolute inset-0 blur-3xl opacity-10 transition-colors duration-1000 ${junkSize > 0 ? 'bg-[#F43F5E]' : 'bg-[#10B981]'}`} />
          <span className={`text-6xl font-black tracking-tighter relative z-10 ${junkSize > 0 ? 'text-[#F43F5E]' : 'text-[#10B981]'}`}>
            {junkSize.toFixed(1)}<span className="text-2xl ml-1">GB</span>
          </span>
        </div>

        <div className="w-full space-y-4">
          <div className="flex justify-between text-[10px] font-black text-slate-500 uppercase tracking-widest">
            <span>System Cache</span>
            <span className={theme === 'dark' ? 'text-white' : 'text-slate-900'}>420 MB</span>
          </div>
          <div className={`w-full h-2 rounded-full overflow-hidden border transition-colors ${theme === 'dark' ? 'bg-black/20 border-white/5' : 'bg-slate-100 border-slate-200'}`}>
            <motion.div 
              initial={{ width: '0%' }}
              animate={{ width: junkSize > 0 ? '85%' : '0%' }}
              transition={{ duration: 1, ease: "circOut" }}
              className="h-full bg-gradient-to-r from-[#F43F5E] to-[#E11D48]" 
            />
          </div>
        </div>
      </div>

      <motion.button 
        whileHover={{ scale: 1.02, boxShadow: '0 20px 40px rgba(0,0,0,0.05)' }}
        whileTap={{ scale: 0.98 }}
        onClick={handleClean}
        disabled={isCleaning || junkSize === 0}
        className={`w-full py-5 rounded-2xl font-black tracking-widest transition-all shadow-xl relative z-10 ${isCleaning || junkSize === 0 ? (theme === 'dark' ? 'bg-slate-800 text-slate-600' : 'bg-slate-100 text-slate-400') : 'bg-[#F43F5E] text-white shadow-[0_15px_30px_rgba(244,63,94,0.2)]'}`}
      >
        {isCleaning ? (
          <div className="flex items-center justify-center gap-3">
            <Activity className="w-5 h-5 animate-spin" />
            PURGING...
          </div>
        ) : junkSize === 0 ? 'STORAGE OPTIMIZED' : 'INITIALIZE PURGE'}
      </motion.button>
    </motion.div>
  );
}

function ThermalPage({ onBack, temp, isCoolDown, onToggle, theme }: { onBack: () => void, temp: number, isCoolDown: boolean, onToggle: () => void, theme: string }) {
  return (
    <motion.div 
      initial={{ opacity: 0, x: -50 }}
      animate={{ opacity: 1, x: 0 }}
      className="p-6 h-full flex flex-col items-center relative"
    >
      <BackgroundEffects theme={theme} />
      
      <div className="flex items-center gap-4 mb-8 w-full relative z-10">
        <Smartphone className="w-6 h-6 text-[#38BDF8] rotate-180 cursor-pointer" onClick={onBack} />
        <h2 className={`text-xl font-black tracking-tighter uppercase ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>Thermal Shield</h2>
      </div>
      
      <div className={`border rounded-[2.5rem] p-10 w-full flex flex-col items-center mb-8 relative overflow-hidden transition-colors ${theme === 'dark' ? 'bg-[#1E293B] border-white/5 shadow-2xl' : 'bg-white border-slate-100 shadow-[0_20px_50px_-15px_rgba(0,0,0,0.05)]'}`}>
        <div className="absolute top-0 left-0 w-full h-full opacity-[0.03] pointer-events-none">
          <div className="grid grid-cols-6 h-full">
            {[...Array(24)].map((_, i) => (
              <div key={i} className={`border ${theme === 'dark' ? 'border-white' : 'border-slate-900'}`} />
            ))}
          </div>
        </div>

        <span className="text-[10px] font-black text-slate-500 uppercase tracking-[0.4em] mb-4">Core Temperature</span>
        <div className="relative mb-8">
          <motion.div 
            animate={{ 
              scale: [1, 1.1, 1],
              opacity: [0.1, 0.2, 0.1]
            }}
            transition={{ repeat: Infinity, duration: 3 }}
            className={`absolute inset-0 blur-3xl transition-colors duration-1000 ${temp > 45 ? 'bg-[#F43F5E]' : 'bg-[#38BDF8]'}`}
          />
          <div className="flex items-baseline relative z-10">
            <span className={`text-7xl font-black tracking-tighter ${temp > 45 ? 'text-[#F43F5E]' : 'text-[#38BDF8]'}`}>
              {temp.toFixed(0)}
            </span>
            <span className="text-2xl font-black text-slate-500 ml-1">°C</span>
          </div>
        </div>

        <div className="w-full grid grid-cols-3 gap-2">
          {[...Array(15)].map((_, i) => (
            <div key={i} className={`h-1.5 rounded-full ${i < (temp - 20) / 2 ? (temp > 45 ? 'bg-[#F43F5E]' : 'bg-[#38BDF8]') : (theme === 'dark' ? 'bg-white/5' : 'bg-slate-100')}`} />
          ))}
        </div>
      </div>

      <div className={`border rounded-2xl p-5 w-full flex justify-between items-center group transition-colors relative z-10 ${theme === 'dark' ? 'bg-[#1E293B] border-white/5 shadow-2xl' : 'bg-white border-slate-100 shadow-sm'}`}>
        <div className="flex flex-col">
          <span className={`text-xs font-black uppercase tracking-widest ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>Auto Dissipation</span>
          <span className="text-[8px] text-slate-500 uppercase font-bold">Background Thermal Control</span>
        </div>
        <div 
          onClick={onToggle}
          className={`w-12 h-6 rounded-full relative cursor-pointer transition-all duration-300 ${isCoolDown ? 'bg-[#38BDF8]' : 'bg-slate-200'}`}
        >
          <motion.div 
            animate={{ x: isCoolDown ? 26 : 4 }} 
            className="absolute top-1 w-4 h-4 bg-white rounded-full shadow-md" 
          />
        </div>
      </div>
    </motion.div>
  );
}

function GenericFeaturePage({ onBack, title, isEnabled, onToggle, theme }: { onBack: () => void, title: string, isEnabled: boolean, onToggle: () => void, theme: string }) {
  const [isApplying, setIsApplying] = useState(false);

  const handleApply = () => {
    setIsApplying(true);
    setTimeout(() => {
      setIsApplying(false);
    }, 1500);
  };

  return (
    <motion.div 
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="p-6 h-full flex flex-col items-center relative"
    >
      <BackgroundEffects theme={theme} />
      
      <div className="flex items-center gap-4 mb-8 w-full relative z-10">
        <Smartphone className="w-6 h-6 text-[#38BDF8] rotate-180 cursor-pointer" onClick={onBack} />
        <h2 className={`text-xl font-black tracking-tighter uppercase ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>{title}</h2>
      </div>
      
      <div className={`border rounded-[2.5rem] p-10 w-full flex flex-col items-center mb-8 relative transition-colors ${theme === 'dark' ? 'bg-[#1E293B] border-white/5 shadow-2xl' : 'bg-white border-slate-100 shadow-[0_20px_50px_-15px_rgba(0,0,0,0.05)]'}`}>
        <div className={`relative p-8 rounded-full mb-6 ${isEnabled ? 'bg-[#10B981]/10' : 'bg-[#38BDF8]/10'}`}>
          <Settings className={`w-16 h-16 relative z-10 ${isEnabled ? 'text-[#10B981]' : 'text-[#38BDF8]'}`} />
        </div>
        
        <p className="text-center text-[10px] font-bold text-slate-500 uppercase tracking-widest mb-8 leading-relaxed">
          This unit calibrates system parameters for peak efficiency and stability.
        </p>

        <div className={`w-full flex justify-between items-center p-5 rounded-2xl border transition-colors ${theme === 'dark' ? 'bg-black/20 border-white/5' : 'bg-slate-50 border-slate-100'}`}>
          <div className="flex flex-col">
            <span className={`text-xs font-black uppercase tracking-widest ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>Master Switch</span>
            <span className="text-[8px] text-slate-500 uppercase font-bold">Toggle Optimization</span>
          </div>
          <div 
            onClick={onToggle}
            className={`w-12 h-6 rounded-full relative cursor-pointer transition-all duration-300 ${isEnabled ? 'bg-[#38BDF8]' : 'bg-slate-200'}`}
          >
            <motion.div 
              animate={{ x: isEnabled ? 26 : 4 }}
              className="absolute top-1 w-4 h-4 bg-white rounded-full shadow-md" 
            />
          </div>
        </div>
      </div>

      <motion.button 
        whileHover={{ scale: 1.02, boxShadow: '0 20px 40px rgba(0,0,0,0.05)' }}
        whileTap={{ scale: 0.98 }}
        onClick={handleApply}
        disabled={isApplying}
        className={`w-full py-5 rounded-2xl font-black tracking-widest transition-all shadow-xl ${isApplying ? (theme === 'dark' ? 'bg-slate-800 text-slate-600' : 'bg-slate-100 text-slate-400') : (theme === 'dark' ? 'bg-white text-slate-900 shadow-[0_15px_30px_rgba(255,255,255,0.05)]' : 'bg-slate-900 text-white shadow-[0_15px_30px_rgba(0,0,0,0.1)]')}`}
      >
        {isApplying ? (
          <div className="flex items-center justify-center gap-3">
            <Activity className="w-5 h-5 animate-spin" />
            CALIBRATING...
          </div>
        ) : 'APPLY PARAMETERS'}
      </motion.button>
    </motion.div>
  );
}

function LagFixerPage({ onBack, isEnabled, onToggle, theme }: { onBack: () => void, isEnabled: boolean, onToggle: () => void, theme: string }) {
  const [isFixing, setIsFixing] = useState(false);

  const handleFix = () => {
    setIsFixing(true);
    setTimeout(() => {
      setIsFixing(false);
    }, 2000);
  };

  return (
    <motion.div 
      initial={{ opacity: 0, y: -20 }}
      animate={{ opacity: 1, y: 0 }}
      className="p-6 h-full flex flex-col items-center relative"
    >
      <BackgroundEffects theme={theme} />
      
      <div className="flex items-center gap-4 mb-8 w-full relative z-10">
        <Smartphone className="w-6 h-6 text-[#38BDF8] rotate-180 cursor-pointer" onClick={onBack} />
        <h2 className={`text-xl font-black tracking-tighter uppercase ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>Network Node</h2>
      </div>
      
      <div className={`border rounded-[2.5rem] p-10 w-full flex flex-col items-center mb-8 relative overflow-hidden transition-colors ${theme === 'dark' ? 'bg-[#1E293B] border-white/5 shadow-2xl' : 'bg-white border-slate-100 shadow-[0_20px_50px_-15px_rgba(0,0,0,0.05)]'}`}>
        <div className="absolute inset-0 opacity-5">
          <div className="w-full h-full bg-[radial-gradient(#38BDF8_1px,transparent_1px)] [background-size:16px_16px]" />
        </div>

        <div className={`relative p-8 rounded-full mb-6 ${isEnabled ? 'bg-[#10B981]/10' : 'bg-[#38BDF8]/10'}`}>
          <Zap className={`w-16 h-16 relative z-10 ${isEnabled ? 'text-[#10B981]' : 'text-[#38BDF8]'}`} />
        </div>

        <p className="text-center text-[10px] font-bold text-slate-500 uppercase tracking-widest mb-8 leading-relaxed">
          Reduces system latency and UI lag by optimizing animation scales and background processes.
        </p>

        <div className={`w-full flex justify-between items-center p-5 rounded-2xl border transition-colors ${theme === 'dark' ? 'bg-black/20 border-white/5' : 'bg-slate-50 border-slate-100'}`}>
          <div className="flex flex-col">
            <span className={`text-xs font-black uppercase tracking-widest ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>Lag Suppression</span>
            <span className="text-[8px] text-slate-500 uppercase font-bold">Active Optimization</span>
          </div>
          <div 
            onClick={onToggle}
            className={`w-12 h-6 rounded-full relative cursor-pointer transition-all duration-300 ${isEnabled ? 'bg-[#38BDF8]' : 'bg-slate-200'}`}
          >
            <motion.div 
              animate={{ x: isEnabled ? 26 : 4 }}
              className="absolute top-1 w-4 h-4 bg-white rounded-full shadow-md" 
            />
          </div>
        </div>
      </div>

      <motion.button 
        whileHover={{ scale: 1.02, boxShadow: '0 20px 40px rgba(0,0,0,0.05)' }}
        whileTap={{ scale: 0.98 }}
        onClick={handleFix}
        disabled={isFixing}
        className={`w-full py-5 rounded-2xl font-black tracking-widest transition-all shadow-xl ${isFixing ? (theme === 'dark' ? 'bg-slate-800 text-slate-600' : 'bg-slate-100 text-slate-400') : 'bg-[#10B981] text-white shadow-[0_15px_30px_rgba(16,185,129,0.2)]'}`}
      >
        {isFixing ? (
          <div className="flex items-center justify-center gap-3">
            <Activity className="w-5 h-5 animate-pulse" />
            STABILIZING...
          </div>
        ) : 'INITIALIZE FIX'}
      </motion.button>
    </motion.div>
  );
}

function AdminDashboard({ onBack, theme }: { onBack: () => void, theme: string }) {
  const [sessions, setSessions] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [confirmDelete, setConfirmDelete] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ message: string, type: 'success' | 'error' } | null>(null);

  useEffect(() => {
    if (feedback) {
      const timer = setTimeout(() => setFeedback(null), 3000);
      return () => clearTimeout(timer);
    }
  }, [feedback]);

  useEffect(() => {
    const q = query(collection(db, 'sessions'), orderBy('createdAt', 'desc'));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const sessList = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
      setSessions(sessList);
      setLoading(false);
    }, (error) => {
      handleFirestoreError(error, OperationType.LIST, 'sessions');
    });
    return () => unsubscribe();
  }, []);

  const toggleSession = async (id: string, currentStatus: string) => {
    try {
      const sessionRef = doc(db, 'sessions', id);
      await updateDoc(sessionRef, {
        status: currentStatus === 'active' ? 'expired' : 'active',
        lastActive: new Date().toISOString()
      });
      setFeedback({ message: `Session ${currentStatus === 'active' ? 'terminated' : 'reactivated'}`, type: 'success' });
    } catch (error) {
      setFeedback({ message: 'Update failed', type: 'error' });
      handleFirestoreError(error, OperationType.UPDATE, `sessions/${id}`);
    }
  };

  const deleteSession = async (id: string) => {
    try {
      const sessionRef = doc(db, 'sessions', id);
      await deleteDoc(sessionRef);
      setConfirmDelete(null);
      setFeedback({ message: 'Session deleted permanently', type: 'success' });
    } catch (error) {
      setFeedback({ message: 'Deletion failed', type: 'error' });
      handleFirestoreError(error, OperationType.DELETE, `sessions/${id}`);
    }
  };

  const seedTestOtp = async () => {
    try {
      const otpDocRef = doc(db, 'otps', '123456');
      await setDoc(otpDocRef, {
        isUsed: false,
        createdAt: new Date().toISOString()
      });
      setFeedback({ message: 'Test OTP (123456) seeded!', type: 'success' });
    } catch (error) {
      setFeedback({ message: 'Seeding failed', type: 'error' });
      handleFirestoreError(error, OperationType.WRITE, 'otps/123456');
    }
  };

  const seedUpdateConfig = async () => {
    try {
      const configRef = doc(db, 'app_config', 'version_info');
      await setDoc(configRef, {
        latest_version_code: 2,
        latest_version_name: '1.2.0',
        update_required: false,
        update_url: 'https://www.mediafire.com/file/example/UltraOptimizeX.apk/file'
      });
      setFeedback({ message: 'Update Config (v1.2.0) seeded!', type: 'success' });
    } catch (error) {
      setFeedback({ message: 'Update seeding failed', type: 'error' });
      handleFirestoreError(error, OperationType.WRITE, 'app_config/version_info');
    }
  };

  return (
    <motion.div 
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      className="p-6 h-full flex flex-col items-center relative"
    >
      <BackgroundEffects theme={theme} />
      
      <div className="flex items-center justify-between mb-8 w-full relative z-10">
        <div className="flex items-center gap-4">
          <Smartphone className="w-6 h-6 text-[#38BDF8] rotate-180 cursor-pointer" onClick={onBack} />
          <h2 className={`text-xl font-black tracking-tighter uppercase ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>Session Manager</h2>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={seedUpdateConfig}
            title="Seed Update Config"
            className={`p-2 rounded-xl transition-all ${theme === 'dark' ? 'bg-white/5 text-slate-400 hover:bg-white/10 hover:text-white' : 'bg-slate-100 text-slate-500 hover:bg-slate-200 hover:text-slate-900'}`}
          >
            <Zap className="w-5 h-5" />
          </button>
          <button
            onClick={seedTestOtp}
            title="Seed Test OTP"
            className={`p-2 rounded-xl transition-all ${theme === 'dark' ? 'bg-white/5 text-slate-400 hover:bg-white/10 hover:text-white' : 'bg-slate-100 text-slate-500 hover:bg-slate-200 hover:text-slate-900'}`}
          >
            <KeyRound className="w-5 h-5" />
          </button>
        </div>
      </div>

      <div className={`w-full rounded-[2.5rem] p-6 border flex-1 overflow-hidden flex flex-col relative z-10 transition-colors ${theme === 'dark' ? 'bg-[#1E293B] border-white/5 shadow-2xl' : 'bg-white border-slate-100 shadow-[0_20px_50px_-15px_rgba(0,0,0,0.05)]'}`}>
        <div className="flex items-center justify-between mb-6">
          <span className={`text-[10px] font-black uppercase tracking-widest ${theme === 'dark' ? 'text-slate-500' : 'text-slate-400'}`}>Active Connections</span>
          <div className="flex items-center gap-2">
            <AnimatePresence>
              {feedback && (
                <motion.span
                  initial={{ opacity: 0, x: 10 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -10 }}
                  className={`text-[8px] font-bold uppercase tracking-widest ${feedback.type === 'success' ? 'text-[#10B981]' : 'text-red-500'}`}
                >
                  {feedback.message}
                </motion.span>
              )}
            </AnimatePresence>
            <div className="px-2 py-1 bg-[#10B981]/10 rounded-md">
              <span className="text-[8px] font-bold text-[#10B981]">{sessions.filter(s => s.status === 'active').length} ONLINE</span>
            </div>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto space-y-3 pr-2 custom-scrollbar">
          {loading ? (
            <div className="flex items-center justify-center h-40">
              <Activity className="w-6 h-6 animate-spin text-[#38BDF8]" />
            </div>
          ) : sessions.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-40 opacity-40">
              <Database className="w-8 h-8 mb-2" />
              <span className="text-[10px] font-bold uppercase tracking-widest">No Sessions Found</span>
            </div>
          ) : (
            sessions.map((sess) => (
              <div key={sess.id} className={`p-4 rounded-2xl border flex items-center justify-between transition-colors ${theme === 'dark' ? 'bg-black/20 border-white/5' : 'bg-slate-50 border-slate-100'}`}>
                <div className="flex items-center gap-3">
                  <div className={`w-2 h-2 rounded-full ${sess.status === 'active' ? 'bg-[#10B981] shadow-[0_0_8px_#10B981]' : 'bg-slate-500'}`} />
                  <div className="flex flex-col">
                    <span className={`text-xs font-bold ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>{sess.username}</span>
                    <span className="text-[8px] text-slate-500 font-mono">{sess.id.substring(0, 12)}...</span>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => toggleSession(sess.id, sess.status)}
                    className={`px-3 py-1.5 rounded-lg text-[8px] font-black uppercase tracking-widest transition-all ${sess.status === 'active' ? 'bg-[#F43F5E]/10 text-[#F43F5E] hover:bg-[#F43F5E] hover:text-white' : 'bg-[#10B981]/10 text-[#10B981] hover:bg-[#10B981] hover:text-white'}`}
                  >
                    {sess.status === 'active' ? 'Terminate' : 'Reactivate'}
                  </button>
                  {confirmDelete === sess.id ? (
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => deleteSession(sess.id)}
                        className="p-1.5 rounded-lg bg-red-500 text-white hover:bg-red-600 transition-all"
                      >
                        <Check className="w-3.5 h-3.5" />
                      </button>
                      <button
                        onClick={() => setConfirmDelete(null)}
                        className={`p-1.5 rounded-lg transition-all ${theme === 'dark' ? 'bg-white/10 text-white' : 'bg-slate-200 text-slate-900'}`}
                      >
                        <X className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  ) : (
                    <button
                      onClick={() => setConfirmDelete(sess.id)}
                      className={`p-1.5 rounded-lg transition-all ${theme === 'dark' ? 'bg-white/5 text-slate-400 hover:bg-red-500/20 hover:text-red-500' : 'bg-slate-100 text-slate-500 hover:bg-red-50 hover:text-red-500'}`}
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  )}
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </motion.div>
  );
}

function BackgroundEffects({ theme }: { theme: string }) {
  return (
    <div className="absolute inset-0 pointer-events-none overflow-hidden z-0">
      <motion.div
        animate={{
          scale: [1, 1.2, 1],
          opacity: [0.05, 0.1, 0.05],
          rotate: [0, 90, 0],
        }}
        transition={{ duration: 20, repeat: Infinity, ease: "linear" }}
        className={`absolute -top-1/4 -left-1/4 w-full h-full rounded-full blur-[100px] ${theme === 'dark' ? 'bg-[#38BDF8]' : 'bg-[#38BDF8]/30'}`}
      />
      <motion.div
        animate={{
          scale: [1, 1.3, 1],
          opacity: [0.03, 0.08, 0.03],
          rotate: [0, -90, 0],
        }}
        transition={{ duration: 25, repeat: Infinity, ease: "linear" }}
        className={`absolute -bottom-1/4 -right-1/4 w-full h-full rounded-full blur-[100px] ${theme === 'dark' ? 'bg-[#F43F5E]' : 'bg-[#F43F5E]/30'}`}
      />
    </div>
  );
}

function FreeFirePage({ onBack, isEnabled, onToggle, autoBoost, onToggleAuto, sensitivity, onToggleSensitivity, theme }: { 
  onBack: () => void, 
  isEnabled: boolean, 
  onToggle: () => void,
  autoBoost: boolean,
  onToggleAuto: () => void,
  sensitivity: boolean,
  onToggleSensitivity: () => void,
  theme: string
}) {
  const [isApplying, setIsApplying] = useState(false);

  const handleApply = () => {
    setIsApplying(true);
    setTimeout(() => {
      setIsApplying(false);
    }, 2500);
  };

  return (
    <motion.div 
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      className="p-6 h-full flex flex-col items-center relative"
    >
      <BackgroundEffects theme={theme} />
      
      <div className="flex items-center gap-4 mb-8 w-full relative z-10">
        <Smartphone className="w-6 h-6 text-[#38BDF8] rotate-180 cursor-pointer" onClick={onBack} />
        <h2 className={`text-xl font-black tracking-tighter uppercase ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>Combat Tuner</h2>
      </div>
      
      <div className={`border rounded-[2.5rem] w-full flex flex-col items-center mb-6 relative overflow-hidden transition-colors ${theme === 'dark' ? 'bg-[#1E293B] border-white/5 shadow-2xl' : 'bg-white border-slate-100 shadow-[0_20px_50px_-15px_rgba(0,0,0,0.05)]'}`}>
        {/* Thematic Image Background */}
        <div className="absolute top-0 left-0 w-full h-40 opacity-20 pointer-events-none">
          <img 
            src="https://picsum.photos/seed/gaming/800/400" 
            alt="Gaming Background" 
            className="w-full h-full object-cover"
            referrerPolicy="no-referrer"
          />
          <div className={`absolute inset-0 bg-gradient-to-b ${theme === 'dark' ? 'from-transparent to-[#1E293B]' : 'from-transparent to-white'}`} />
        </div>

        <div className="p-10 pt-20 flex flex-col items-center w-full relative z-10">
          <div className={`relative p-8 rounded-full mb-6 ${isEnabled ? 'bg-[#F43F5E]/10' : 'bg-[#38BDF8]/10'}`}>
            <motion.div
              animate={isEnabled ? {
                scale: [1, 1.2, 1],
                opacity: [1, 0.5, 1],
              } : {}}
              transition={{ repeat: Infinity, duration: 2 }}
              className="absolute inset-0 rounded-full border-2 border-[#F43F5E]/30"
            />
            <Target className={`w-16 h-16 relative z-10 ${isEnabled ? 'text-[#F43F5E]' : 'text-[#38BDF8]'}`} />
          </div>

          <div className="w-full space-y-3">
            {[
              { label: 'Engine Optimization', state: isEnabled, toggle: onToggle },
              { label: 'Auto Boost Logic', state: autoBoost, toggle: onToggleAuto },
              { label: 'Sensitivity Calibration', state: sensitivity, toggle: onToggleSensitivity }
            ].map((item, idx) => (
              <div key={idx} className={`flex justify-between items-center p-4 rounded-xl border w-full transition-colors ${theme === 'dark' ? 'bg-black/20 border-white/5' : 'bg-slate-50 border-slate-100'}`}>
                <span className={`text-[10px] font-black uppercase tracking-widest ${theme === 'dark' ? 'text-slate-400' : 'text-slate-700'}`}>{item.label}</span>
                <div 
                  onClick={item.toggle}
                  className={`w-10 h-5 rounded-full relative cursor-pointer transition-all duration-300 ${item.state ? 'bg-[#F43F5E]' : 'bg-slate-200'}`}
                >
                  <motion.div 
                    animate={{ x: item.state ? 22 : 2 }}
                    className="absolute top-1 w-3 h-3 bg-white rounded-full shadow-sm" 
                  />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      <motion.button 
        whileHover={{ scale: 1.02, boxShadow: '0 20px 40px rgba(0,0,0,0.05)' }}
        whileTap={{ scale: 0.98 }}
        onClick={handleApply}
        disabled={isApplying}
        className={`w-full py-5 rounded-2xl font-black tracking-widest transition-all shadow-xl relative z-10 ${isApplying ? (theme === 'dark' ? 'bg-slate-800 text-slate-600' : 'bg-slate-100 text-slate-400') : 'bg-[#F43F5E] text-white shadow-[0_15px_30px_rgba(244,63,94,0.2)]'}`}
      >
        {isApplying ? (
          <div className="flex items-center justify-center gap-3">
            <Activity className="w-5 h-5 animate-spin" />
            CALIBRATING...
          </div>
        ) : 'INITIALIZE COMBAT MODE'}
      </motion.button>
    </motion.div>
  );
}

function CircularMeter({ value, label, color, unit = '%', theme }: { value: number, label: string, color: string, unit?: string, theme: string }) {
  const radius = 32;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (value / 100) * circumference;

  return (
    <div className="flex flex-col items-center group">
      <div className="relative w-24 h-24 flex items-center justify-center">
        {/* Glow Effect */}
        <div 
          className="absolute inset-0 rounded-full blur-xl opacity-10 transition-opacity group-hover:opacity-20"
          style={{ backgroundColor: color }}
        />
        
        <svg className="w-full h-full transform -rotate-90">
          <circle
            cx="48"
            cy="48"
            r={radius}
            stroke="currentColor"
            strokeWidth="6"
            fill="transparent"
            className={theme === 'dark' ? 'text-white/5' : 'text-slate-100'}
          />
          <motion.circle
            cx="48"
            cy="48"
            r={radius}
            stroke={color}
            strokeWidth="6"
            fill="transparent"
            strokeDasharray={circumference}
            initial={{ strokeDashoffset: circumference }}
            animate={{ strokeDashoffset: offset }}
            transition={{ duration: 1.5, ease: "circOut" }}
            strokeLinecap="round"
          />
        </svg>
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <span className={`text-sm font-black tracking-tighter ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>{value.toFixed(0)}{unit}</span>
        </div>
      </div>
      <span className={`text-[10px] font-black mt-2 uppercase tracking-[0.2em] transition-colors ${theme === 'dark' ? 'text-slate-500 group-hover:text-white' : 'text-slate-400 group-hover:text-slate-900'}`}>
        {label}
      </span>
    </div>
  );
}

function GameToolsPage({ onBack, fpsEnabled, onToggleFps, crosshairEnabled, onToggleCrosshair, theme }: { 
  onBack: () => void, 
  fpsEnabled: boolean, 
  onToggleFps: () => void,
  crosshairEnabled: boolean,
  onToggleCrosshair: () => void,
  theme: string
}) {
  return (
    <motion.div 
      initial={{ opacity: 0, x: 50 }}
      animate={{ opacity: 1, x: 0 }}
      className="p-6 h-full flex flex-col items-center relative"
    >
      <BackgroundEffects theme={theme} />
      
      <div className="flex items-center gap-4 mb-8 w-full relative z-10">
        <Smartphone className="w-6 h-6 text-[#38BDF8] rotate-180 cursor-pointer" onClick={onBack} />
        <h2 className={`text-xl font-black tracking-tighter uppercase ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>Tactical Gear</h2>
      </div>
      
      <div className={`border rounded-[2.5rem] p-8 w-full mb-6 relative overflow-hidden transition-colors ${theme === 'dark' ? 'bg-[#1E293B] border-white/5 shadow-2xl' : 'bg-white border-slate-100 shadow-[0_20px_50px_-15px_rgba(0,0,0,0.05)]'}`}>
        <div className="absolute top-0 right-0 p-6 opacity-5 pointer-events-none">
          <Crosshair size={120} className={theme === 'dark' ? 'text-white' : 'text-slate-900'} />
        </div>

        <div className="space-y-6 relative z-10">
          <div className="flex justify-between items-center group">
            <div className="flex items-center gap-4">
              <div className={`p-3 rounded-xl transition-colors ${fpsEnabled ? 'bg-[#38BDF8]/10 text-[#38BDF8]' : (theme === 'dark' ? 'bg-white/5 text-slate-500' : 'bg-slate-50 text-slate-400')}`}>
                <Activity size={20} />
              </div>
              <div className="flex flex-col">
                <span className={`text-xs font-black uppercase tracking-widest ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>FPS Overlay</span>
                <span className="text-[8px] text-slate-500 uppercase font-bold">Real-time Performance</span>
              </div>
            </div>
            <div 
              onClick={onToggleFps}
              className={`w-12 h-6 rounded-full relative cursor-pointer transition-all duration-300 ${fpsEnabled ? 'bg-[#38BDF8]' : 'bg-slate-200'}`}
            >
              <motion.div 
                animate={{ x: fpsEnabled ? 26 : 4 }}
                className="absolute top-1 w-4 h-4 bg-white rounded-full shadow-md" 
              />
            </div>
          </div>
          
          <div className={`h-[1px] ${theme === 'dark' ? 'bg-white/5' : 'bg-slate-100'}`} />
          
          <div className="flex justify-between items-center group">
            <div className="flex items-center gap-4">
              <div className={`p-3 rounded-xl transition-colors ${crosshairEnabled ? 'bg-[#38BDF8]/10 text-[#38BDF8]' : (theme === 'dark' ? 'bg-white/5 text-slate-500' : 'bg-slate-50 text-slate-400')}`}>
                <Crosshair size={20} />
              </div>
              <div className="flex flex-col">
                <span className={`text-xs font-black uppercase tracking-widest ${theme === 'dark' ? 'text-white' : 'text-slate-900'}`}>Crosshair Pro</span>
                <span className="text-[8px] text-slate-500 uppercase font-bold">Custom Aiming Node</span>
              </div>
            </div>
            <div 
              onClick={onToggleCrosshair}
              className={`w-12 h-6 rounded-full relative cursor-pointer transition-all duration-300 ${crosshairEnabled ? 'bg-[#38BDF8]' : 'bg-slate-200'}`}
            >
              <motion.div 
                animate={{ x: crosshairEnabled ? 26 : 4 }}
                className="absolute top-1 w-4 h-4 bg-white rounded-full shadow-md" 
              />
            </div>
          </div>
        </div>
      </div>

      {/* Crosshair Preview */}
      <div className={`w-full h-32 rounded-[2rem] mb-6 flex items-center justify-center relative overflow-hidden border transition-colors ${theme === 'dark' ? 'bg-black/40 border-white/5' : 'bg-slate-50 border-slate-100'}`}>
        <img 
          src="https://picsum.photos/seed/aim/400/200" 
          alt="Aim Preview" 
          className="absolute inset-0 w-full h-full object-cover opacity-20 grayscale"
          referrerPolicy="no-referrer"
        />
        {crosshairEnabled && (
          <motion.div 
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            className="relative z-10"
          >
            <div className="w-10 h-10 relative flex items-center justify-center">
              <div className="absolute w-full h-0.5 bg-[#38BDF8]" />
              <div className="absolute h-full w-0.5 bg-[#38BDF8]" />
              <div className="w-2 h-2 bg-[#38BDF8] rounded-full shadow-[0_0_10px_#38BDF8]" />
            </div>
          </motion.div>
        )}
        {!crosshairEnabled && <span className="text-[8px] font-black text-slate-500 uppercase tracking-widest relative z-10">Preview Disabled</span>}
      </div>

      <div className="w-full relative z-10">
        <h3 className="text-[10px] font-black text-slate-500 uppercase mb-4 tracking-[0.3em] ml-2">Optic Profiles</h3>
        <div className="flex gap-4 overflow-x-auto pb-4 no-scrollbar">
          {[1, 2, 3, 4, 5].map(i => (
            <motion.div 
              key={i} 
              whileHover={{ scale: 1.05, backgroundColor: theme === 'dark' ? 'rgba(255,255,255,0.02)' : 'rgba(0,0,0,0.02)' }}
              className={`min-w-[70px] h-[70px] rounded-2xl border flex items-center justify-center cursor-pointer transition-colors shadow-sm ${theme === 'dark' ? 'bg-[#1E293B] border-white/5' : 'bg-white border-slate-100'}`}
            >
              <div className="w-5 h-5 border-2 border-[#38BDF8] rounded-full relative opacity-40">
                <div className="absolute inset-0 flex items-center justify-center">
                  <div className="w-1 h-1 bg-[#38BDF8] rounded-full" />
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </div>
    </motion.div>
  );
}

function QuickAction({ icon, label, onClick, theme }: { icon: React.ReactNode, label: string, onClick?: () => void, theme: string }) {
  return (
    <motion.div 
      variants={{
        hidden: { opacity: 0, y: 10 },
        show: { opacity: 1, y: 0 }
      }}
      whileHover={{ scale: 1.02, backgroundColor: theme === 'dark' ? 'rgba(56, 189, 248, 0.05)' : 'rgba(0,0,0,0.02)' }}
      whileTap={{ scale: 0.98 }}
      onClick={onClick}
      className={`rounded-2xl p-4 flex flex-col items-center justify-center cursor-pointer transition-all group border ${theme === 'dark' ? 'bg-[#1E293B] border-white/5 hover:border-[#38BDF8]/30 shadow-2xl' : 'bg-white border-slate-100 hover:border-[#38BDF8]/30 shadow-sm'}`}
    >
      <div className={`mb-2 transition-colors group-hover:text-[#38BDF8] group-hover:drop-shadow-[0_0_8px_rgba(56,189,248,0.3)] ${theme === 'dark' ? 'text-slate-500' : 'text-slate-400'}`}>
        {React.cloneElement(icon as React.ReactElement, { size: 24 })}
      </div>
      <span className={`text-[10px] font-black uppercase tracking-wider transition-colors text-center ${theme === 'dark' ? 'text-slate-500 group-hover:text-white' : 'text-slate-400 group-hover:text-slate-900'}`}>
        {label}
      </span>
    </motion.div>
  );
}
